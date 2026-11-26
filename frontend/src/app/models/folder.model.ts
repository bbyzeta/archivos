export interface FolderRequest {
  name: string;
}

export interface Folder {
  id: string;
  name: string;
  publicSlug: string;
  publicUrl: string;
  createdAt: string;
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

export interface PublicFolder {
  name: string;
  publicSlug: string;
  files: FolderFile[];
}
