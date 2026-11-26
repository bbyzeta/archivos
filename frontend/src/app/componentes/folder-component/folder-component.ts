import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, forkJoin, from, map, mergeMap, tap } from 'rxjs';
import { Folder, FolderDirectory, FolderFile } from '../../models/folder.model';
import { ExplorerPreferencesService } from '../../services/explorer-preferences.service';
import { FolderService } from '../../services/folder.service';
import { AlertaService } from '../alerts/alert-component';
import { BreadcrumbComponent } from '../breadcrumb-component/breadcrumb-component';
import { ColorPickerComponent } from '../color-picker-component/color-picker-component';
import { ExplorerToolbarComponent } from '../explorer-toolbar-component/explorer-toolbar-component';
import { ExplorerViewComponent } from '../explorer-view-component/explorer-view-component';
import { NavbarComponent } from '../navbar-component/navbar-component';
import { FolderEntry, isSameOrInside, joinPath, listDirectory, replacePathPrefix, sortListing } from '../../utils/file-explorer';
import { copyToClipboard, publicPageUrl } from '../../utils/file-format';
import { DEFAULT_FOLDER_COLOR } from '../../utils/folder-colors';

/** Subidas simultáneas máximas. */
const MAX_CONCURRENT_UPLOADS = 3;

/** Nombre de carpeta válido: sin "." / "..", separadores ni caracteres no permitidos en disco. */
const NEW_FOLDER_NAME_PATTERN = /^(?!\.{1,2}$)[^\\/:*?"<>|\u0000-\u001f]+$/;

/** Máximo de archivos por lote (evita saturar la cola y la interfaz con carpetas enormes). */
const MAX_FILES_PER_BATCH = 1000;

interface PendingFile {
  file: File;
  /** Carpeta dentro de la carpeta destino, p. ej. "Docs/2026"; vacío para la raíz. */
  relativePath: string;
}

interface UploadItem {
  id: number;
  file: File;
  relativePath: string;
  status: 'pending' | 'uploading' | 'done' | 'error';
  progress: number;
  error?: string;
}

@Component({
  selector: 'app-folder',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NavbarComponent, BreadcrumbComponent, ColorPickerComponent,
    ExplorerToolbarComponent, ExplorerViewComponent],
  templateUrl: './folder-component.html',
  styleUrl: './folder-component.scss'
})
export class FolderComponent {
  private readonly folderService = inject(FolderService);
  private readonly alertService = inject(AlertaService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly prefs = inject(ExplorerPreferencesService);
  private readonly folderId = this.route.snapshot.paramMap.get('id') ?? '';

  /** Subcarpeta que se está viendo (query param `path`); vacío = raíz. Permite deep links y botón Atrás. */
  readonly currentPath = toSignal(
    this.route.queryParamMap.pipe(map((params) => params.get('path') ?? '')),
    { initialValue: this.route.snapshot.queryParamMap.get('path') ?? '' });

  private readonly uploadQueue$ = new Subject<UploadItem>();
  private nextUploadId = 0;

  readonly folder = signal<Folder | null>(null);
  readonly files = signal<FolderFile[]>([]);
  readonly loading = signal(true);
  readonly dragging = signal(false);
  readonly uploads = signal<UploadItem[]>([]);
  readonly copiedKey = signal<string | null>(null);

  /** Subcarpetas registradas explícitamente (incluye las vacías). */
  readonly directories = signal<FolderDirectory[]>([]);
  readonly listing = computed(() => sortListing(
    listDirectory(this.files(), this.currentPath(), this.directories()), this.prefs.folderSort()));

  readonly summary = computed(() => {
    const { folders, files } = this.listing();
    return `${folders.length} ${folders.length === 1 ? 'carpeta' : 'carpetas'} · ${files.length} ${files.length === 1 ? 'archivo' : 'archivos'}`;
  });

  /** Archivos seleccionados (selección múltiple para eliminar). */
  readonly selected = signal<ReadonlySet<string>>(new Set());
  readonly defaultFolderColor = DEFAULT_FOLDER_COLOR;

  readonly newFolderOpen = signal(false);
  readonly creatingFolder = signal(false);
  /** Subcarpeta que se está renombrando; null = el modal crea una nueva. */
  readonly renamingDirectory = signal<FolderEntry | null>(null);
  private readonly fb = inject(FormBuilder);
  readonly newFolderForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(NEW_FOLDER_NAME_PATTERN)]]
  });

  constructor() {
    // Una sola cola: garantiza máximo 3 subidas en paralelo aunque se agreguen lotes nuevos
    this.uploadQueue$
      .pipe(mergeMap((item) => this.upload(item), MAX_CONCURRENT_UPLOADS), takeUntilDestroyed())
      .subscribe();

    this.loadFolder();
  }

  // ---------- selección y drag & drop ----------

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.enqueue(Array.from(input.files ?? []).map((file) => ({ file, relativePath: '' })));
    input.value = ''; // permite volver a elegir los mismos archivos
  }

  /** Carpeta elegida con el selector: cada archivo trae su ruta en webkitRelativePath ("Carpeta/sub/archivo.txt"). */
  onFolderSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.enqueue(Array.from(input.files ?? []).map((file) => ({
      file,
      relativePath: this.dirname(file.webkitRelativePath)
    })));
    input.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  onDragLeave(): void {
    this.dragging.set(false);
  }

  async onDrop(event: DragEvent): Promise<void> {
    event.preventDefault();
    this.dragging.set(false);

    // Las entradas deben leerse de forma síncrona: el DataTransfer se vacía tras el primer await
    const dropped = Array.from(event.dataTransfer?.items ?? [])
      .filter((item) => item.kind === 'file')
      .map((item) => ({ entry: item.webkitGetAsEntry?.() ?? null, file: item.getAsFile() }));

    const found: PendingFile[] = [];
    const emptyDirs: string[] = [];
    for (const { entry, file } of dropped) {
      if (entry) {
        await this.collectEntry(entry, '', found, emptyDirs);
      } else if (file) {
        found.push({ file, relativePath: '' });
      }
    }
    this.enqueue(found);
    this.createEmptyDirectories(emptyDirs);
  }

  // ---------- subcarpetas ----------

  openNewFolder(): void {
    this.renamingDirectory.set(null);
    this.newFolderForm.reset();
    this.newFolderOpen.set(true);
  }

  openRenameDirectory(entry: FolderEntry): void {
    this.renamingDirectory.set(entry);
    this.newFolderForm.reset({ name: entry.name });
    this.newFolderOpen.set(true);
  }

  closeNewFolder(): void {
    if (!this.creatingFolder()) this.newFolderOpen.set(false);
  }

  esNombreCarpetaInvalido(): boolean {
    const control = this.newFolderForm.controls.name;
    return control.invalid && (control.dirty || control.touched);
  }

  /** Crea una subcarpeta o, si hay una en edición, la renombra (mismo modal). */
  submitFolderForm(): void {
    const name = this.newFolderForm.controls.name.value.trim();
    if (this.newFolderForm.invalid || !name) {
      this.newFolderForm.markAllAsTouched();
      return;
    }

    const target = this.renamingDirectory();
    this.creatingFolder.set(true);

    if (target) {
      this.folderService.renameDirectory(this.folderId, target.path, name).subscribe({
        next: ({ path: newPath }) => {
          this.applyRename(target.path, newPath);
          this.creatingFolder.set(false);
          this.newFolderOpen.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.creatingFolder.set(false);
          this.alertService.mostrarAlertaError(err.status === 409
            ? 'Ya existe una carpeta con ese nombre.'
            : 'No se pudo renombrar la carpeta. Intenta de nuevo.');
        }
      });
      return;
    }

    this.folderService.createDirectory(this.folderId, joinPath(this.currentPath(), name)).subscribe({
      next: ({ path }) => {
        this.addDirectory(path);
        this.creatingFolder.set(false);
        this.newFolderOpen.set(false);
      },
      error: () => {
        this.creatingFolder.set(false);
        this.alertService.mostrarAlertaError('No se pudo crear la carpeta. Intenta de nuevo.');
      }
    });
  }

  async deleteDirectory(entry: FolderEntry): Promise<void> {
    const message = entry.fileCount === 0
      ? `Se eliminará la carpeta vacía "${entry.name}".`
      : `Se eliminará "${entry.name}" con sus ${entry.fileCount} ${entry.fileCount === 1 ? 'archivo' : 'archivos'} y subcarpetas. Esta acción no se puede deshacer.`;
    const confirmed = await this.alertService.confirmarAccion('¿Eliminar carpeta?', message, 'Sí, eliminar');
    if (!confirmed) return;

    this.folderService.deleteDirectory(this.folderId, entry.path, true).subscribe({
      next: () => {
        this.directories.update((list) => list.filter((d) => !isSameOrInside(d.path, entry.path)));
        this.files.update((list) => list.filter((f) => !isSameOrInside(f.relativePath, entry.path)));
      },
      error: () => this.alertService.mostrarAlertaError('No se pudo eliminar la carpeta.')
    });
  }

  /** Refleja en pantalla el renombrado: actualiza las rutas de archivos y subcarpetas afectadas. */
  private applyRename(oldPath: string, newPath: string): void {
    const rename = (p: string) => (isSameOrInside(p, oldPath) ? replacePathPrefix(p, oldPath, newPath) : p);
    this.directories.update((list) => list.map((d) => ({ ...d, path: rename(d.path) })));
    this.files.update((list) => list.map((f) => {
      if (!isSameOrInside(f.relativePath, oldPath)) return f;
      const relativePath = rename(f.relativePath);
      return { ...f, relativePath, publicUrl: this.rebuildPublicUrl(f, oldPath, relativePath) };
    }));
  }

  /** Los links directos incluyen la ruta: se reconstruyen con la nueva ruta (segmentos codificados). */
  private rebuildPublicUrl(file: FolderFile, oldPath: string, newRelativePath: string): string {
    const marker = '/file/';
    const cut = file.publicUrl.indexOf(marker);
    if (cut < 0) return file.publicUrl;
    const base = file.publicUrl.substring(0, cut + marker.length);
    const segments = [...newRelativePath.split('/').filter(Boolean), file.filename];
    return base + segments.map(encodeURIComponent).join('/');
  }

  private addDirectory(path: string): void {
    this.directories.update((list) => (list.some((d) => d.path === path) ? list : [...list, { path, color: null }]));
  }

  /** Registra en el servidor las carpetas vacías detectadas al soltar, para que no se pierdan. */
  private createEmptyDirectories(paths: string[]): void {
    if (paths.length === 0) return;
    const base = this.currentPath();
    let failed = 0;

    from(paths).pipe(
      mergeMap((path) => this.folderService.createDirectory(this.folderId, joinPath(base, path)).pipe(
        catchError(() => {
          failed++;
          return EMPTY;
        })
      ), MAX_CONCURRENT_UPLOADS)
    ).subscribe({
      next: ({ path }) => this.addDirectory(path),
      complete: () => {
        if (failed > 0) {
          this.alertService.mostrarAlertaError(`No se pudieron crear ${failed} carpeta(s) vacía(s).`);
        }
      }
    });
  }

  /** Navega a una subcarpeta ('' = raíz) conservando la vista en la URL. */
  openFolder(path: string): void {
    this.clearSelection();
    this.router.navigate([], { relativeTo: this.route, queryParams: { path: path || null } });
  }

  clearFinished(): void {
    this.uploads.update((list) => list.filter((u) => u.status === 'pending' || u.status === 'uploading'));
  }

  // ---------- acciones sobre archivos ----------

  async deleteFile(file: FolderFile): Promise<void> {
    const confirmed = await this.alertService.confirmarAccion(
      '¿Eliminar archivo?', `Se eliminará "${file.filename}" de forma permanente.`, 'Sí, eliminar');
    if (!confirmed) return;

    this.folderService.deleteFile(this.folderId, file.id).subscribe({
      next: () => this.files.update((list) => list.filter((f) => f.id !== file.id)),
      error: () => this.alertService.mostrarAlertaError('No se pudo eliminar el archivo.')
    });
  }

  copyLink(key: string, url: string): void {
    copyToClipboard(url);
    this.copiedKey.set(key);
    setTimeout(() => {
      if (this.copiedKey() === key) this.copiedKey.set(null);
    }, 1800);
  }

  // ---------- presentación (utilidades compartidas) ----------

  readonly publicPageUrl = publicPageUrl;

  // ---------- selección múltiple ----------

  toggleSelect(id: string): void {
    this.selected.update((current) => {
      const next = new Set(current);
      if (!next.delete(id)) next.add(id);
      return next;
    });
  }

  /** Selecciona (o deselecciona) todos los archivos del nivel actual. */
  toggleSelectAll(): void {
    const ids = this.listing().files.map((f) => f.id);
    const allSelected = ids.length > 0 && ids.every((id) => this.selected().has(id));
    this.selected.set(allSelected ? new Set() : new Set(ids));
  }

  clearSelection(): void {
    if (this.selected().size > 0) this.selected.set(new Set());
  }

  async deleteSelected(): Promise<void> {
    const targets = this.files().filter((f) => this.selected().has(f.id));
    if (targets.length === 0) return;

    const confirmed = await this.alertService.confirmarAccion(
      '¿Eliminar archivos?',
      `Se eliminarán ${targets.length} ${targets.length === 1 ? 'archivo' : 'archivos'} de forma permanente.`,
      'Sí, eliminar');
    if (!confirmed) return;

    const deleted = new Set<string>();
    from(targets).pipe(
      mergeMap((file) => this.folderService.deleteFile(this.folderId, file.id).pipe(
        tap(() => deleted.add(file.id)),
        catchError(() => EMPTY)
      ), MAX_CONCURRENT_UPLOADS)
    ).subscribe({
      complete: () => {
        this.files.update((list) => list.filter((f) => !deleted.has(f.id)));
        this.selected.set(new Set());
        if (deleted.size < targets.length) {
          this.alertService.mostrarAlertaError(`No se pudieron eliminar ${targets.length - deleted.size} archivo(s).`);
        }
      }
    });
  }

  // ---------- color ----------

  setRootColor(color: string | null): void {
    this.folderService.setColor(this.folderId, color).subscribe({
      next: (updated) => this.folder.set(updated),
      error: () => this.alertService.mostrarAlertaError('No se pudo cambiar el color de la carpeta.')
    });
  }

  setSubfolderColor(entry: FolderEntry, color: string | null): void {
    this.folderService.setDirectoryColor(this.folderId, entry.path, color).subscribe({
      next: (saved) => this.directories.update((list) => list.some((d) => d.path === saved.path)
        ? list.map((d) => (d.path === saved.path ? saved : d))
        : [...list, saved]),
      error: () => this.alertService.mostrarAlertaError('No se pudo cambiar el color de la carpeta.')
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.newFolderOpen()) this.closeNewFolder();
  }

  // ---------- internos ----------

  /** Recorre recursivamente un archivo o directorio soltado y acumula sus archivos con su ruta relativa. */
  private async collectEntry(entry: FileSystemEntry, parentPath: string, out: PendingFile[],
      emptyDirs: string[]): Promise<void> {
    if (out.length > MAX_FILES_PER_BATCH) return;

    if (entry.isFile) {
      const file = await new Promise<File | null>((resolve) =>
        (entry as FileSystemFileEntry).file((f) => resolve(f), () => resolve(null)));
      if (file) out.push({ file, relativePath: parentPath });
      return;
    }

    if (entry.isDirectory) {
      const path = parentPath ? `${parentPath}/${entry.name}` : entry.name;
      const reader = (entry as FileSystemDirectoryEntry).createReader();
      // readEntries devuelve el contenido por lotes: hay que repetir hasta recibir una lista vacía
      let batch: FileSystemEntry[];
      let children = 0;
      do {
        batch = await new Promise<FileSystemEntry[]>((resolve) => reader.readEntries(resolve, () => resolve([])));
        children += batch.length;
        for (const child of batch) {
          await this.collectEntry(child, path, out, emptyDirs);
        }
      } while (batch.length > 0);

      // Una carpeta sin contenido no genera archivos: se registra aparte para conservarla
      if (children === 0) emptyDirs.push(path);
    }
  }

  private dirname(path: string): string {
    const cut = path.lastIndexOf('/');
    return cut < 0 ? '' : path.substring(0, cut);
  }

  private enqueue(pending: PendingFile[]): void {
    if (pending.length === 0) return;
    if (pending.length > MAX_FILES_PER_BATCH) {
      this.alertService.mostrarAlerta('Demasiados archivos',
        `Se pueden subir hasta ${MAX_FILES_PER_BATCH} archivos a la vez. Sube la carpeta por partes.`,
        'warning', 'Entendido', '#13322b');
      return;
    }

    // Lo que se sube se guarda dentro de la subcarpeta que se está viendo
    const base = this.currentPath();
    for (const { file, relativePath: relative } of pending) {
      const relativePath = joinPath(base, relative);
      const item: UploadItem = { id: this.nextUploadId++, file, relativePath, status: 'pending', progress: 0 };
      this.uploads.update((list) => [...list, item]);
      this.uploadQueue$.next(item);
    }
  }

  private upload(item: UploadItem) {
    this.patchUpload(item.id, { status: 'uploading' });

    return this.folderService.uploadFile(this.folderId, item.file, item.relativePath).pipe(
      tap((event) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.patchUpload(item.id, { progress: Math.round((event.loaded / event.total) * 100) });
        } else if (event.type === HttpEventType.Response && event.body) {
          this.patchUpload(item.id, { status: 'done', progress: 100 });
          this.files.update((list) => [event.body as FolderFile, ...list]);
        }
      }),
      catchError((err: HttpErrorResponse) => {
        this.patchUpload(item.id, { status: 'error', error: this.uploadErrorMessage(err) });
        return EMPTY;
      })
    );
  }

  private uploadErrorMessage(err: HttpErrorResponse): string {
    switch (err.status) {
      case 409: return 'Ya existe un archivo con ese nombre.';
      case 413: return 'El archivo excede el tamaño máximo.';
      case 400: return 'Archivo o nombre no válido.';
      case 0: return 'Sin conexión con el servidor.';
      default: return 'No se pudo subir el archivo.';
    }
  }

  private patchUpload(id: number, changes: Partial<UploadItem>): void {
    this.uploads.update((list) => list.map((u) => (u.id === id ? { ...u, ...changes } : u)));
  }

  private loadFolder(): void {
    this.folderService.get(this.folderId).subscribe({
      next: (folder) => {
        this.folder.set(folder);
        forkJoin({
          files: this.folderService.listFiles(this.folderId),
          directories: this.folderService.listDirectories(this.folderId)
        }).subscribe({
          next: ({ files, directories }) => {
            this.files.set(files);
            this.directories.set(directories);
            this.loading.set(false);
          },
          error: () => this.failLoading()
        });
      },
      error: () => this.failLoading()
    });
  }

  private failLoading(): void {
    this.loading.set(false);
    this.alertService.mostrarAlertaError('No se pudo cargar la carpeta.');
  }
}
