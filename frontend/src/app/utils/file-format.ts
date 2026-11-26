import { FolderFile } from '../models/folder.model';

/** Utilidades de presentación de archivos compartidas por las vistas de carpeta. */

export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const units = ['KB', 'MB', 'GB', 'TB'];
  let value = bytes / 1024;
  let i = 0;
  while (value >= 1024 && i < units.length - 1) {
    value /= 1024;
    i++;
  }
  return `${value.toFixed(value < 10 ? 1 : 0)} ${units[i]}`;
}

export function fileType(file: FolderFile): string {
  const dot = file.filename.lastIndexOf('.');
  return dot > 0 ? file.filename.substring(dot + 1).toUpperCase() : 'Archivo';
}

export function fileIcon(file: FolderFile): string {
  const mime = file.mimeType;
  if (mime.startsWith('image/')) return 'bi-file-earmark-image';
  if (mime.startsWith('video/')) return 'bi-file-earmark-play';
  if (mime.startsWith('audio/')) return 'bi-file-earmark-music';
  if (mime === 'application/pdf') return 'bi-file-earmark-pdf';
  if (/zip|rar|7z|tar|gzip/.test(mime)) return 'bi-file-earmark-zip';
  if (/word|document/.test(mime)) return 'bi-file-earmark-word';
  if (/excel|spreadsheet|csv/.test(mime)) return 'bi-file-earmark-excel';
  if (/powerpoint|presentation/.test(mime)) return 'bi-file-earmark-slides';
  if (mime.startsWith('text/')) return 'bi-file-earmark-text';
  return 'bi-file-earmark';
}

/** Mismos tipos que el backend muestra en línea (imagen, audio, vídeo, PDF y texto plano). */
export function isPreviewable(file: FolderFile): boolean {
  const mime = file.mimeType;
  return mime === 'application/pdf' || mime === 'text/plain'
    || mime.startsWith('image/') || mime.startsWith('audio/') || mime.startsWith('video/');
}

/** navigator.clipboard solo existe en HTTPS/localhost; se usa execCommand como alternativa. */
export function copyToClipboard(text: string): void {
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(text).catch(() => legacyCopy(text));
  } else {
    legacyCopy(text);
  }
}

function legacyCopy(text: string): void {
  const area = document.createElement('textarea');
  area.value = text;
  area.style.position = 'fixed';
  area.style.opacity = '0';
  document.body.appendChild(area);
  area.select();
  document.execCommand('copy');
  document.body.removeChild(area);
}

/** Link de la página pública de una carpeta (vista del frontend, no el JSON de la API). */
export function publicPageUrl(slug: string): string {
  return `${window.location.origin}/public/${slug}`;
}
