import { FolderFile } from '../models/folder.model';

/** Lógica de explorador: deriva carpetas y subcarpetas a partir de la lista plana con `relativePath`. */

export interface FolderEntry {
  name: string;
  /** Ruta completa de la subcarpeta, p. ej. "Docs/2026". */
  path: string;
  /** Archivos contenidos (incluye subcarpetas). */
  fileCount: number;
  sizeBytes: number;
  lastUploadedAt: string;
}

export interface DirectoryListing {
  folders: FolderEntry[];
  files: FolderFile[];
}

export interface Crumb {
  name: string;
  path: string;
}

/** Contenido directo de `currentPath`: sus subcarpetas y los archivos que están justo en ese nivel. */
export function listDirectory(allFiles: FolderFile[], currentPath: string): DirectoryListing {
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
      lastUploadedAt: file.uploadedAt
    };
    entry.fileCount++;
    entry.sizeBytes += file.sizeBytes;
    if (file.uploadedAt > entry.lastUploadedAt) entry.lastUploadedAt = file.uploadedAt;
    folders.set(name, entry);
  }

  const byName = (a: string, b: string) => a.localeCompare(b, 'es', { sensitivity: 'base', numeric: true });
  return {
    folders: [...folders.values()].sort((a, b) => byName(a.name, b.name)),
    files: files.sort((a, b) => byName(a.filename, b.filename))
  };
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
