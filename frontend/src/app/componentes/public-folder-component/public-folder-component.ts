import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { FolderFile, PublicFolder } from '../../models/folder.model';
import { ExplorerPreferencesService } from '../../services/explorer-preferences.service';
import { PublicFolderService } from '../../services/public-folder.service';
import { listDirectory, sortListing } from '../../utils/file-explorer';
import { copyToClipboard } from '../../utils/file-format';
import { DEFAULT_FOLDER_COLOR } from '../../utils/folder-colors';
import { BreadcrumbComponent } from '../breadcrumb-component/breadcrumb-component';
import { ExplorerToolbarComponent } from '../explorer-toolbar-component/explorer-toolbar-component';
import { ExplorerViewComponent } from '../explorer-view-component/explorer-view-component';

/** Vista pública de una carpeta: se abre desde el link compartido, sin iniciar sesión. */
@Component({
  selector: 'app-public-folder',
  standalone: true,
  imports: [BreadcrumbComponent, ExplorerToolbarComponent, ExplorerViewComponent],
  templateUrl: './public-folder-component.html',
  styleUrl: './public-folder-component.scss'
})
export class PublicFolderComponent {
  private readonly publicFolderService = inject(PublicFolderService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly slug = this.route.snapshot.paramMap.get('slug') ?? '';
  protected readonly prefs = inject(ExplorerPreferencesService);

  /** Subcarpeta que se está viendo (query param `path`); vacío = raíz. */
  readonly currentPath = toSignal(
    this.route.queryParamMap.pipe(map((params) => params.get('path') ?? '')),
    { initialValue: this.route.snapshot.queryParamMap.get('path') ?? '' });

  readonly folder = signal<PublicFolder | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly search = signal('');
  readonly copiedId = signal<string | null>(null);
  readonly rootColor = computed(() => this.folder()?.color ?? DEFAULT_FOLDER_COLOR);

  readonly listing = computed(() => sortListing(
    listDirectory(this.folder()?.files ?? [], this.currentPath(), this.folder()?.directories ?? []),
    this.prefs.folderSort()));

  /** Con búsqueda activa se listan los coincidentes de todo el árbol, con su ruta. */
  readonly searchResults = computed(() => {
    const term = this.search().trim().toLowerCase();
    if (!term) return null;
    const matches = (this.folder()?.files ?? [])
      .filter((f) => `${f.relativePath}/${f.filename}`.toLowerCase().includes(term));
    return sortListing({ folders: [], files: matches }, this.prefs.folderSort()).files;
  });

  readonly summary = computed(() => {
    const results = this.searchResults();
    if (results) return `${results.length} ${results.length === 1 ? 'resultado' : 'resultados'}`;
    const { folders, files } = this.listing();
    return `${folders.length} ${folders.length === 1 ? 'carpeta' : 'carpetas'} · ${files.length} ${files.length === 1 ? 'archivo' : 'archivos'}`;
  });

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
