export interface FolderRequest {
  name: string;
}

export interface Folder {
  id: string;
  name: string;
  publicSlug: string;
  publicUrl: string;
  createdAt: string;
  /** Color hexadecimal #RRGGBB para identificar la carpeta; null = color por defecto. */
  color: string | null;
}

export interface FolderFile {
  id: string;
  filename: string;
  relativePath: string;
  mimeType: string;
  sizeBytes: number;
  uploadedAt: string;
  publicUrl: string;
}

/** Subcarpeta registrada explícitamente (carpetas vacías o con color). */
export interface FolderDirectory {
  path: string;
  color: string | null;
}

export interface PublicFolder {
  name: string;
  publicSlug: string;
  color: string | null;
  files: FolderFile[];
  directories: FolderDirectory[];
}
