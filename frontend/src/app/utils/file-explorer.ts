import { FolderDirectory, FolderFile } from '../models/folder.model';

/** Lógica de explorador: deriva carpetas y subcarpetas a partir de la lista plana con `relativePath`. */

export interface FolderEntry {
  name: string;
  /** Ruta completa de la subcarpeta, p. ej. "Docs/2026". */
  path: string;
  /** Archivos contenidos (incluye subcarpetas). */
  fileCount: number;
  sizeBytes: number;
  lastUploadedAt: string;
  /** Color asignado (hex) o null para el color por defecto. */
  color: string | null;
}

export interface DirectoryListing {
  folders: FolderEntry[];
  files: FolderFile[];
}

export interface Crumb {
  name: string;
  path: string;
}

export type SortKey = 'name' | 'date' | 'size' | 'type';
export type SortDir = 'asc' | 'desc';

export interface SortState {
  key: SortKey;
  dir: SortDir;
}

const byName = (a: string, b: string) => a.localeCompare(b, 'es', { sensitivity: 'base', numeric: true });

/** Contenido directo de `currentPath`: sus subcarpetas y los archivos que están justo en ese nivel. */
export function listDirectory(
  allFiles: FolderFile[], currentPath: string, directories: FolderDirectory[] = []
): DirectoryListing {
  const prefix = currentPath ? `${currentPath}/` : '';
  const folders = new Map<string, FolderEntry>();
  const files: FolderFile[] = [];

  for (const file of allFiles) {
    if (file.relativePath === currentPath) {
      files.push(file);
      continue;
    }
    if (!file.relativePath.startsWith(prefix)) continue;

    const name = file.relativePath.substring(prefix.length).split('/')[0];
    const entry = folders.get(name) ?? {
      name,
      path: prefix + name,
      fileCount: 0,
      sizeBytes: 0,
      lastUploadedAt: file.uploadedAt,
      color: null
    };
    entry.fileCount++;
    entry.sizeBytes += file.sizeBytes;
    if (file.uploadedAt > entry.lastUploadedAt) entry.lastUploadedAt = file.uploadedAt;
    folders.set(name, entry);
  }

  // Subcarpetas registradas explícitamente (pueden estar vacías o tener color)
  for (const dir of directories) {
    if (dir.path === currentPath || !dir.path.startsWith(prefix)) continue;
    const name = dir.path.substring(prefix.length).split('/')[0];
    if (!folders.has(name)) {
      folders.set(name, { name, path: prefix + name, fileCount: 0, sizeBytes: 0, lastUploadedAt: '', color: null });
    }
  }

  // El color pertenece a la subcarpeta exacta (no se hereda de sus hijas)
  const colors = new Map(directories.map((d) => [d.path, d.color]));
  for (const entry of folders.values()) {
    entry.color = colors.get(entry.path) ?? null;
  }

  return sortListing({ folders: [...folders.values()], files }, { key: 'name', dir: 'asc' });
}

/** Ordena carpetas y archivos (las carpetas siempre van primero). Los empates se resuelven por nombre. */
export function sortListing(listing: DirectoryListing, sort: SortState): DirectoryListing {
  const sign = sort.dir === 'asc' ? 1 : -1;

  const folderValue = (f: FolderEntry): string | number => {
    switch (sort.key) {
      case 'date': return f.lastUploadedAt;
      case 'size': return f.sizeBytes;
      default: return f.name; // todas son del mismo tipo: "Carpeta"
    }
  };
  const fileValue = (f: FolderFile): string | number => {
    switch (sort.key) {
      case 'date': return f.uploadedAt;
      case 'size': return f.sizeBytes;
      case 'type': return extensionOf(f.filename);
      default: return f.filename;
    }
  };

  return {
    folders: [...listing.folders].sort((a, b) => compare(folderValue(a), folderValue(b), sign) || byName(a.name, b.name)),
    files: [...listing.files].sort((a, b) => compare(fileValue(a), fileValue(b), sign) || byName(a.filename, b.filename))
  };
}

/** Ordena carpetas raíz por nombre o por fecha de creación. */
export function sortByKey<T>(items: T[], sort: SortState, name: (item: T) => string, date: (item: T) => string): T[] {
  const sign = sort.dir === 'asc' ? 1 : -1;
  return [...items].sort((a, b) => sort.key === 'date'
    ? compare(date(a), date(b), sign) || byName(name(a), name(b))
    : compare(name(a), name(b), sign));
}

function compare(a: string | number, b: string | number, sign: number): number {
  if (typeof a === 'number' && typeof b === 'number') return (a - b) * sign;
  return byName(String(a), String(b)) * sign;
}

function extensionOf(filename: string): string {
  const dot = filename.lastIndexOf('.');
  return dot > 0 ? filename.substring(dot + 1).toLowerCase() : '';
}

export function breadcrumbs(path: string): Crumb[] {
  if (!path) return [];
  const segments = path.split('/');
  return segments.map((name, i) => ({ name, path: segments.slice(0, i + 1).join('/') }));
}

export function parentPath(path: string): string {
  const cut = path.lastIndexOf('/');
  return cut < 0 ? '' : path.substring(0, cut);
}

export function joinPath(base: string, child: string): string {
  if (!base) return child;
  return child ? `${base}/${child}` : base;
}

/** ¿`path` es `base` o está dentro de ella? */
export function isSameOrInside(path: string, base: string): boolean {
  return path === base || path.startsWith(`${base}/`);
}

/** Cambia el prefijo `oldBase` por `newBase` en `path` (que debe ser igual a oldBase o estar dentro). */
export function replacePathPrefix(path: string, oldBase: string, newBase: string): string {
  return newBase + path.substring(oldBase.length);
}
