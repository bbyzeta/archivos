import { Injectable, signal } from '@angular/core';
import { SortDir, SortKey, SortState } from '../utils/file-explorer';

export type ViewMode = 'grid' | 'list';

const STORAGE_KEY = 'explorer.prefs';
const SORT_KEYS: SortKey[] = ['name', 'date', 'size', 'type'];

interface StoredPrefs {
  view: ViewMode;
  homeSort: SortState;
  folderSort: SortState;
}

const DEFAULTS: StoredPrefs = {
  view: 'grid',
  homeSort: { key: 'name', dir: 'asc' },
  folderSort: { key: 'name', dir: 'asc' }
};

/** Preferencias del explorador (vista y orden) guardadas en el navegador. */
@Injectable({ providedIn: 'root' })
export class ExplorerPreferencesService {
  private readonly stored = this.read();

  /** Vista cuadrícula o lista, compartida por Inicio, carpeta y vista pública. */
  readonly view = signal<ViewMode>(this.stored.view);
  /** Orden de las carpetas en Inicio (nombre o fecha). */
  readonly homeSort = signal<SortState>(this.stored.homeSort);
  /** Orden dentro de una carpeta (nombre, fecha, tamaño o tipo). */
  readonly folderSort = signal<SortState>(this.stored.folderSort);

  setView(view: ViewMode): void {
    this.view.set(view);
    this.persist();
  }

  setHomeSort(sort: SortState): void {
    this.homeSort.set(sort);
    this.persist();
  }

  setFolderSort(sort: SortState): void {
    this.folderSort.set(sort);
    this.persist();
  }

  private persist(): void {
    try {
      const prefs: StoredPrefs = { view: this.view(), homeSort: this.homeSort(), folderSort: this.folderSort() };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs));
    } catch {
      // Almacenamiento no disponible (modo privado, cuota): la preferencia solo dura la sesión
    }
  }

  private read(): StoredPrefs {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return DEFAULTS;
      const parsed = JSON.parse(raw) as Partial<StoredPrefs>;
      return {
        view: parsed.view === 'list' ? 'list' : 'grid',
        homeSort: this.validSort(parsed.homeSort, DEFAULTS.homeSort),
        folderSort: this.validSort(parsed.folderSort, DEFAULTS.folderSort)
      };
    } catch {
      return DEFAULTS;
    }
  }

  private validSort(value: Partial<SortState> | undefined, fallback: SortState): SortState {
    const key = SORT_KEYS.includes(value?.key as SortKey) ? (value?.key as SortKey) : fallback.key;
    const dir: SortDir = value?.dir === 'desc' ? 'desc' : 'asc';
    return { key, dir };
  }
}
