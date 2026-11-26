import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, forkJoin, from, map, mergeMap, tap } from 'rxjs';
import { Folder, FolderFile } from '../../models/folder.model';
import { FolderService } from '../../services/folder.service';
import { AlertaService } from '../alerts/alert-component';
import { BreadcrumbComponent } from '../breadcrumb-component/breadcrumb-component';
import { NavbarComponent } from '../navbar-component/navbar-component';
import { FolderEntry, joinPath, listDirectory } from '../../utils/file-explorer';
import { copyToClipboard, fileIcon, fileType, formatSize, publicPageUrl } from '../../utils/file-format';

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
  imports: [DatePipe, ReactiveFormsModule, RouterLink, NavbarComponent, BreadcrumbComponent],
  templateUrl: './folder-component.html',
  styleUrl: './folder-component.scss'
})
export class FolderComponent {
  private readonly folderService = inject(FolderService);
  private readonly alertService = inject(AlertaService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
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
  readonly directories = signal<string[]>([]);
  readonly listing = computed(() => listDirectory(this.files(), this.currentPath(), this.directories()));

  readonly newFolderOpen = signal(false);
  readonly creatingFolder = signal(false);
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
    this.newFolderForm.reset();
    this.newFolderOpen.set(true);
  }

  closeNewFolder(): void {
    if (!this.creatingFolder()) this.newFolderOpen.set(false);
  }

  esNombreCarpetaInvalido(): boolean {
    const control = this.newFolderForm.controls.name;
    return control.invalid && (control.dirty || control.touched);
  }

  createFolder(): void {
    const name = this.newFolderForm.controls.name.value.trim();
    if (this.newFolderForm.invalid || !name) {
      this.newFolderForm.markAllAsTouched();
      return;
    }

    this.creatingFolder.set(true);
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
    const confirmed = await this.alertService.confirmarAccion(
      '¿Eliminar carpeta?', `Se eliminará la carpeta vacía "${entry.name}".`, 'Sí, eliminar');
    if (!confirmed) return;

    this.folderService.deleteDirectory(this.folderId, entry.path).subscribe({
      next: () => this.directories.update((list) =>
        list.filter((d) => d !== entry.path && !d.startsWith(`${entry.path}/`))),
      error: () => this.alertService.mostrarAlertaError('No se pudo eliminar la carpeta.')
    });
  }

  private addDirectory(path: string): void {
    this.directories.update((list) => (list.includes(path) ? list : [...list, path]));
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

  readonly formatSize = formatSize;
  readonly fileType = fileType;
  readonly fileIcon = fileIcon;
  readonly publicPageUrl = publicPageUrl;

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
