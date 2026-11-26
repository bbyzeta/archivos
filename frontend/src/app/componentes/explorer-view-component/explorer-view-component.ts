import { Component, computed, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FolderFile } from '../../models/folder.model';
import { ViewMode } from '../../services/explorer-preferences.service';
import { FolderEntry } from '../../utils/file-explorer';
import { fileIcon, fileType, formatSize, isPreviewable } from '../../utils/file-format';
import { DEFAULT_FOLDER_COLOR } from '../../utils/folder-colors';
import { ColorPickerComponent } from '../color-picker-component/color-picker-component';

/**
 * Contenido de una carpeta (subcarpetas y archivos) en vista de lista o cuadrícula.
 * Lo usan la vista de administración (`admin = true`, con acciones de gestión) y la vista pública.
 */
@Component({
  selector: 'app-explorer-view',
  standalone: true,
  imports: [DatePipe, ColorPickerComponent],
  templateUrl: './explorer-view-component.html',
  styleUrl: './explorer-view-component.scss'
})
export class ExplorerViewComponent {
  readonly folders = input.required<FolderEntry[]>();
  readonly files = input.required<FolderFile[]>();
  readonly view = input.required<ViewMode>();
  /** Muestra las acciones de gestión (color, renombrar, eliminar y selección múltiple). */
  readonly admin = input(false);
  readonly selected = input<ReadonlySet<string>>(new Set());
  readonly copiedId = input<string | null>(null);
  /** Antepone la ruta al nombre del archivo (resultados de búsqueda en todo el árbol). */
  readonly showPath = input(false);

  readonly openFolder = output<string>();
  readonly renameFolder = output<FolderEntry>();
  readonly deleteFolder = output<FolderEntry>();
  readonly colorFolder = output<{ entry: FolderEntry; color: string | null }>();
  readonly copyLink = output<FolderFile>();
  readonly deleteFile = output<FolderFile>();
  readonly toggleSelect = output<string>();
  readonly toggleSelectAll = output<void>();

  protected readonly allSelected = computed(() =>
    this.files().length > 0 && this.files().every((f) => this.selected().has(f.id)));

  protected readonly formatSize = formatSize;
  protected readonly fileType = fileType;
  protected readonly fileIcon = fileIcon;
  protected readonly isPreviewable = isPreviewable;

  protected folderColor(entry: FolderEntry): string {
    return entry.color ?? DEFAULT_FOLDER_COLOR;
  }

  protected countLabel(entry: FolderEntry): string {
    return `${entry.fileCount} ${entry.fileCount === 1 ? 'archivo' : 'archivos'}`;
  }
}
