import { FolderFile } from '../models/folder.model';
import { fileIcon, fileType, formatSize, isPreviewable, publicPageUrl } from './file-format';

const base: FolderFile = {
  id: '1', filename: 'a.txt', relativePath: '', mimeType: 'text/plain', sizeBytes: 0, uploadedAt: '', publicUrl: ''
};
const withFile = (changes: Partial<FolderFile>): FolderFile => ({ ...base, ...changes });

describe('formatSize', () => {
  it('formatea bytes, KB, MB y GB', () => {
    expect(formatSize(0)).toBe('0 B');
    expect(formatSize(1023)).toBe('1023 B');
    expect(formatSize(1024)).toBe('1.0 KB');
    expect(formatSize(1536)).toBe('1.5 KB');
    expect(formatSize(10 * 1024)).toBe('10 KB');
    expect(formatSize(5 * 1024 * 1024)).toBe('5.0 MB');
    expect(formatSize(3 * 1024 ** 3)).toBe('3.0 GB');
  });
});

describe('fileType', () => {
  it('usa la extensión en mayúsculas', () => {
    expect(fileType(withFile({ filename: 'Constancia.pdf' }))).toBe('PDF');
    expect(fileType(withFile({ filename: 'archivo.tar.gz' }))).toBe('GZ');
  });

  it('sin extensión o con nombre oculto devuelve "Archivo"', () => {
    expect(fileType(withFile({ filename: 'LEEME' }))).toBe('Archivo');
    expect(fileType(withFile({ filename: '.gitignore' }))).toBe('Archivo');
  });
});

describe('fileIcon', () => {
  it('elige el icono según el tipo MIME', () => {
    expect(fileIcon(withFile({ mimeType: 'image/png' }))).toBe('bi-file-earmark-image');
    expect(fileIcon(withFile({ mimeType: 'application/pdf' }))).toBe('bi-file-earmark-pdf');
    expect(fileIcon(withFile({ mimeType: 'application/zip' }))).toBe('bi-file-earmark-zip');
    expect(fileIcon(withFile({ mimeType: 'text/csv' }))).toBe('bi-file-earmark-excel');
    expect(fileIcon(withFile({ mimeType: 'application/x-desconocido' }))).toBe('bi-file-earmark');
  });
});

describe('isPreviewable', () => {
  it('coincide con los tipos que el backend muestra en línea', () => {
    for (const mimeType of ['image/png', 'audio/mpeg', 'video/mp4', 'application/pdf', 'text/plain']) {
      expect(isPreviewable(withFile({ mimeType }))).toBe(true);
    }
  });

  it('los tipos activos o desconocidos solo se descargan', () => {
    for (const mimeType of ['text/html', 'application/zip', 'application/javascript', 'text/csv']) {
      expect(isPreviewable(withFile({ mimeType }))).toBe(false);
    }
  });
});

describe('publicPageUrl', () => {
  it('arma el link de la página pública con el origen actual', () => {
    expect(publicPageUrl('abc123')).toBe(`${window.location.origin}/public/abc123`);
  });
});
