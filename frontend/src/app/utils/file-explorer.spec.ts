import { FolderFile } from '../models/folder.model';
import { breadcrumbs, isSameOrInside, joinPath, listDirectory, parentPath, replacePathPrefix } from './file-explorer';

function file(relativePath: string, filename: string, sizeBytes = 10, uploadedAt = '2026-01-01T10:00:00'): FolderFile {
  return {
    id: `${relativePath}/${filename}`, filename, relativePath, sizeBytes, uploadedAt,
    mimeType: 'text/plain', publicUrl: `http://x/file/${relativePath}/${filename}`
  };
}

describe('listDirectory', () => {
  const files = [
    file('', 'raiz.txt'),
    file('Docs', 'a.txt', 100, '2026-01-02T10:00:00'),
    file('Docs/2026', 'b.txt', 50, '2026-03-01T10:00:00'),
    file('Docs/2026/Q1', 'c.txt', 5),
    file('Docs2', 'otro.txt'),
    file('Fotos', 'z.png')
  ];

  it('en la raíz muestra las carpetas de primer nivel y los archivos sueltos', () => {
    const { folders, files: direct } = listDirectory(files, '');

    expect(folders.map((f) => f.name)).toEqual(['Docs', 'Docs2', 'Fotos']);
    expect(direct.map((f) => f.filename)).toEqual(['raiz.txt']);
  });

  it('agrega el conteo, el tamaño y la última fecha incluyendo subcarpetas', () => {
    const docs = listDirectory(files, '').folders.find((f) => f.name === 'Docs')!;

    expect(docs.path).toBe('Docs');
    expect(docs.fileCount).toBe(3);
    expect(docs.sizeBytes).toBe(155);
    expect(docs.lastUploadedAt).toBe('2026-03-01T10:00:00');
  });

  it('dentro de una carpeta muestra solo su contenido directo', () => {
    const { folders, files: direct } = listDirectory(files, 'Docs');

    expect(folders.map((f) => f.path)).toEqual(['Docs/2026']);
    expect(direct.map((f) => f.filename)).toEqual(['a.txt']);
  });

  it('no mezcla carpetas que solo comparten prefijo textual (Docs vs Docs2)', () => {
    const { folders, files: direct } = listDirectory(files, 'Docs');

    expect(folders.some((f) => f.name.startsWith('Docs2'))).toBe(false);
    expect(direct.some((f) => f.filename === 'otro.txt')).toBe(false);
  });

  it('incluye las carpetas vacías registradas explícitamente', () => {
    const { folders } = listDirectory(files, '', [{ path: 'Vacia', color: null }, { path: 'Docs/Sub/Vacia', color: null }]);

    const vacia = folders.find((f) => f.name === 'Vacia')!;
    expect(vacia).toMatchObject({ path: 'Vacia', fileCount: 0, sizeBytes: 0, lastUploadedAt: '', color: null });
    expect(listDirectory(files, 'Docs', [{ path: 'Docs/Sub/Vacia', color: null }]).folders.map((f) => f.name)).toEqual(['2026', 'Sub']);
  });

  it('una carpeta registrada que ya tiene archivos no se duplica', () => {
    const { folders } = listDirectory(files, '', [{ path: 'Docs', color: null }]);

    expect(folders.filter((f) => f.name === 'Docs')).toHaveLength(1);
    expect(folders.find((f) => f.name === 'Docs')!.fileCount).toBe(3);
  });

  it('una carpeta registrada vacía se ve al entrar en ella sin contenido', () => {
    const listing = listDirectory([], 'Vacia', [{ path: 'Vacia', color: null }]);

    expect(listing.folders).toEqual([]);
    expect(listing.files).toEqual([]);
  });

  it('ordena por nombre sin distinguir mayúsculas y con números naturales', () => {
    const listing = listDirectory([file('', 'b10.txt'), file('', 'B2.txt'), file('', 'a.txt')], '');

    expect(listing.files.map((f) => f.filename)).toEqual(['a.txt', 'B2.txt', 'b10.txt']);
  });
});

describe('rutas', () => {
  it('breadcrumbs devuelve cada tramo con su ruta acumulada', () => {
    expect(breadcrumbs('')).toEqual([]);
    expect(breadcrumbs('Docs/2026/Q1')).toEqual([
      { name: 'Docs', path: 'Docs' },
      { name: '2026', path: 'Docs/2026' },
      { name: 'Q1', path: 'Docs/2026/Q1' }
    ]);
  });

  it('parentPath sube un nivel', () => {
    expect(parentPath('Docs/2026')).toBe('Docs');
    expect(parentPath('Docs')).toBe('');
    expect(parentPath('')).toBe('');
  });

  it('joinPath une sin barras sobrantes', () => {
    expect(joinPath('', 'a')).toBe('a');
    expect(joinPath('Docs', 'a/b')).toBe('Docs/a/b');
    expect(joinPath('Docs', '')).toBe('Docs');
  });

  it('isSameOrInside distingue prefijos textuales de ancestros reales', () => {
    expect(isSameOrInside('Docs', 'Docs')).toBe(true);
    expect(isSameOrInside('Docs/2026', 'Docs')).toBe(true);
    expect(isSameOrInside('Docs2', 'Docs')).toBe(false);
    expect(isSameOrInside('Docs2/x', 'Docs')).toBe(false);
  });

  it('replacePathPrefix reemplaza solo el prefijo', () => {
    expect(replacePathPrefix('Docs', 'Docs', 'Informes')).toBe('Informes');
    expect(replacePathPrefix('Docs/2026/Q1', 'Docs', 'Informes')).toBe('Informes/2026/Q1');
    expect(replacePathPrefix('A/Docs/x', 'A/Docs', 'A/Nuevo')).toBe('A/Nuevo/x');
  });
});
