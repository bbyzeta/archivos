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

interface UploadItem {
  id: number;
  file: File;
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
    this.enqueue(Array.from(input.files ?? []));
    input.value = ''; // permite volver a elegir los mismos archivos
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  onDragLeave(): void {
    this.dragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);

    const items = Array.from(event.dataTransfer?.items ?? []);
    const files: File[] = [];
    let skippedFolders = false;

    for (const item of items) {
      if (item.kind !== 'file') continue;
      if (item.webkitGetAsEntry?.()?.isDirectory) {
        skippedFolders = true;
        continue;
      }
      const file = item.getAsFile();
      if (file) files.push(file);
    }

    if (skippedFolders) {
      this.alertService.mostrarAlerta('Aviso', 'Las carpetas no se pueden subir; selecciona archivos.', 'info', 'Entendido', '#13322b');
    }
    this.enqueue(files);
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

  private enqueue(files: File[]): void {
    for (const file of files) {
      const item: UploadItem = { id: this.nextUploadId++, file, status: 'pending', progress: 0 };
      this.uploads.update((list) => [...list, item]);
      this.uploadQueue$.next(item);
    }
  }

  private upload(item: UploadItem) {
    this.patchUpload(item.id, { status: 'uploading' });

    return this.folderService.uploadFile(this.folderId, item.file).pipe(
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
