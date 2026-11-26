import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { FolderFile, PublicFolder } from '../../models/folder.model';
import { PublicFolderService } from '../../services/public-folder.service';
import { copyToClipboard, fileIcon, fileType, formatSize, isPreviewable } from '../../utils/file-format';

/** Vista pública de una carpeta: se abre desde el link compartido, sin iniciar sesión. */
@Component({
  selector: 'app-public-folder',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './public-folder-component.html',
  styleUrl: './public-folder-component.scss'
})
export class PublicFolderComponent {
  private readonly publicFolderService = inject(PublicFolderService);
  private readonly slug = inject(ActivatedRoute).snapshot.paramMap.get('slug') ?? '';

  readonly folder = signal<PublicFolder | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly search = signal('');
  readonly copiedId = signal<string | null>(null);

  readonly visibleFiles = computed(() => {
    const term = this.search().trim().toLowerCase();
    const files = this.folder()?.files ?? [];
    if (!term) return files;
    return files.filter((f) => `${f.relativePath}/${f.filename}`.toLowerCase().includes(term));
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

  copyLink(file: FolderFile): void {
    copyToClipboard(file.publicUrl);
    this.copiedId.set(file.id);
    setTimeout(() => {
      if (this.copiedId() === file.id) this.copiedId.set(null);
    }, 1800);
  }
}
