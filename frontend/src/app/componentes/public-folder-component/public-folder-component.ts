import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { FolderFile, PublicFolder } from '../../models/folder.model';
import { PublicFolderService } from '../../services/public-folder.service';
import { listDirectory } from '../../utils/file-explorer';
import { copyToClipboard, fileIcon, fileType, formatSize, isPreviewable } from '../../utils/file-format';
import { BreadcrumbComponent } from '../breadcrumb-component/breadcrumb-component';

/** Vista pública de una carpeta: se abre desde el link compartido, sin iniciar sesión. */
@Component({
  selector: 'app-public-folder',
  standalone: true,
  imports: [DatePipe, BreadcrumbComponent],
  templateUrl: './public-folder-component.html',
  styleUrl: './public-folder-component.scss'
})
export class PublicFolderComponent {
  private readonly publicFolderService = inject(PublicFolderService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly slug = this.route.snapshot.paramMap.get('slug') ?? '';

  /** Subcarpeta que se está viendo (query param `path`); vacío = raíz. */
  readonly currentPath = toSignal(
    this.route.queryParamMap.pipe(map((params) => params.get('path') ?? '')),
    { initialValue: this.route.snapshot.queryParamMap.get('path') ?? '' });

  readonly folder = signal<PublicFolder | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly search = signal('');
  readonly copiedId = signal<string | null>(null);

  readonly listing = computed(() => listDirectory(this.folder()?.files ?? [], this.currentPath()));

  /** Con búsqueda activa se listan los coincidentes de todo el árbol, con su ruta. */
  readonly searchResults = computed(() => {
    const term = this.search().trim().toLowerCase();
    if (!term) return null;
    return (this.folder()?.files ?? []).filter((f) => `${f.relativePath}/${f.filename}`.toLowerCase().includes(term));
  });

  readonly formatSize = formatSize;
  readonly fileType = fileType;
  readonly fileIcon = fileIcon;
  readonly isPreviewable = isPreviewable;

  constructor() {
    this.publicFolderService.get(this.slug).subscribe({
      next: (folder) => {
        this.folder.set(folder);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(err.status === 404
          ? 'Esta carpeta no existe o el enlace ya no es válido.'
          : 'No se pudo cargar la carpeta. Intenta de nuevo más tarde.');
        this.loading.set(false);
      }
    });
  }

  onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  openFolder(path: string): void {
    this.search.set('');
    this.router.navigate([], { relativeTo: this.route, queryParams: { path: path || null } });
  }

  copyLink(file: FolderFile): void {
    copyToClipboard(file.publicUrl);
    this.copiedId.set(file.id);
    setTimeout(() => {
      if (this.copiedId() === file.id) this.copiedId.set(null);
    }, 1800);
  }
}
