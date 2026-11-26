import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, mergeMap, tap } from 'rxjs';
import { Folder, FolderFile } from '../../models/folder.model';
import { FolderService } from '../../services/folder.service';
import { AlertaService } from '../alerts/alert-component';
import { NavbarComponent } from '../navbar-component/navbar-component';
import { copyToClipboard, fileIcon, fileType, formatSize, publicPageUrl } from '../../utils/file-format';

/** Subidas simultáneas máximas. */
const MAX_CONCURRENT_UPLOADS = 3;

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
  imports: [DatePipe, RouterLink, NavbarComponent],
  templateUrl: './folder-component.html',
  styleUrl: './folder-component.scss'
})
export class FolderComponent {
  private readonly folderService = inject(FolderService);
  private readonly alertService = inject(AlertaService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly folderId = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';

  private readonly uploadQueue$ = new Subject<UploadItem>();
  private nextUploadId = 0;

  readonly folder = signal<Folder | null>(null);
  readonly files = signal<FolderFile[]>([]);
  readonly loading = signal(true);
  readonly dragging = signal(false);
  readonly uploads = signal<UploadItem[]>([]);
  readonly copiedKey = signal<string | null>(null);

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
    for (const { entry, file } of dropped) {
      if (entry) {
        await this.collectEntry(entry, '', found);
      } else if (file) {
        found.push({ file, relativePath: '' });
      }
    }
    this.enqueue(found);
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
  private async collectEntry(entry: FileSystemEntry, parentPath: string, out: PendingFile[]): Promise<void> {
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
      do {
        batch = await new Promise<FileSystemEntry[]>((resolve) => reader.readEntries(resolve, () => resolve([])));
        for (const child of batch) {
          await this.collectEntry(child, path, out);
        }
      } while (batch.length > 0);
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

    for (const { file, relativePath } of pending) {
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
        this.folderService.listFiles(this.folderId).subscribe({
          next: (files) => {
            this.files.set(files);
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
