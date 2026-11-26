import { FolderFile } from '../models/folder.model';
import { listDirectory, sortByKey, sortListing } from './file-explorer';

function file(relativePath: string, filename: string, sizeBytes: number, uploadedAt: string): FolderFile {
  return {
    id: `${relativePath}/${filename}`, filename, relativePath, sizeBytes, uploadedAt,
    mimeType: 'text/plain', publicUrl: ''
  };
}

describe('color de subcarpetas', () => {
  const files = [file('Docs', 'a.txt', 1, '2026-01-01T00:00:00'), file('Docs/2026', 'b.txt', 1, '2026-01-01T00:00:00')];

  it('asigna el color de la subcarpeta exacta', () => {
    const { folders } = listDirectory(files, '', [{ path: 'Docs', color: '#9b2247' }]);

    expect(folders[0].color).toBe('#9b2247');
  });

  it('el color no se hereda hacia las subcarpetas hijas ni hacia las padres', () => {
    const root = listDirectory(files, '', [{ path: 'Docs/2026', color: '#1e5b4f' }]);
    const inner = listDirectory(files, 'Docs', [{ path: 'Docs/2026', color: '#1e5b4f' }]);

    expect(root.folders[0].color).toBeNull();
    expect(inner.folders[0].color).toBe('#1e5b4f');
  });

  it('una carpeta vacía con color conserva su color', () => {
    const { folders } = listDirectory([], '', [{ path: 'Vacia', color: '#a57f2c' }]);

    expect(folders).toEqual([expect.objectContaining({ name: 'Vacia', fileCount: 0, color: '#a57f2c' })]);
  });
});

describe('sortListing', () => {
  const files = [
    file('', 'b.pdf', 300, '2026-02-01T00:00:00'),
    file('', 'a.txt', 100, '2026-03-01T00:00:00'),
    file('', 'c.docx', 200, '2026-01-01T00:00:00'),
    file('Grande', 'x.bin', 5000, '2026-01-15T00:00:00'),
    file('Chica', 'y.bin', 10, '2026-04-01T00:00:00')
  ];
  const listing = listDirectory(files, '');
  const names = (l: ReturnType<typeof sortListing>) => l.files.map((f) => f.filename);
  const folderNames = (l: ReturnType<typeof sortListing>) => l.folders.map((f) => f.name);

  it('por nombre ascendente y descendente', () => {
    expect(names(sortListing(listing, { key: 'name', dir: 'asc' }))).toEqual(['a.txt', 'b.pdf', 'c.docx']);
    expect(names(sortListing(listing, { key: 'name', dir: 'desc' }))).toEqual(['c.docx', 'b.pdf', 'a.txt']);
    expect(folderNames(sortListing(listing, { key: 'name', dir: 'desc' }))).toEqual(['Grande', 'Chica']);
  });

  it('por tamaño ordena numéricamente (no como texto)', () => {
    expect(names(sortListing(listing, { key: 'size', dir: 'asc' }))).toEqual(['a.txt', 'c.docx', 'b.pdf']);
    expect(folderNames(sortListing(listing, { key: 'size', dir: 'desc' }))).toEqual(['Grande', 'Chica']);
  });

  it('por fecha usa la fecha de subida (y la última subida en las carpetas)', () => {
    expect(names(sortListing(listing, { key: 'date', dir: 'asc' }))).toEqual(['c.docx', 'b.pdf', 'a.txt']);
    expect(names(sortListing(listing, { key: 'date', dir: 'desc' }))).toEqual(['a.txt', 'b.pdf', 'c.docx']);
    expect(folderNames(sortListing(listing, { key: 'date', dir: 'asc' }))).toEqual(['Grande', 'Chica']);
  });

  it('por tipo agrupa por extensión y desempata por nombre', () => {
    const mixed = listDirectory([
      file('', 'z.txt', 1, ''), file('', 'a.txt', 1, ''), file('', 'm.pdf', 1, ''), file('', 'sin-extension', 1, '')
    ], '');

    expect(names(sortListing(mixed, { key: 'type', dir: 'asc' }))).toEqual(['sin-extension', 'm.pdf', 'a.txt', 'z.txt']);
  });

  it('no modifica la lista original', () => {
    const before = names(listing);

    sortListing(listing, { key: 'size', dir: 'desc' });

    expect(names(listing)).toEqual(before);
  });
});

describe('sortByKey', () => {
  const items = [
    { name: 'beta', date: '2026-01-02' },
    { name: 'Alfa', date: '2026-03-01' },
    { name: 'gamma', date: '2026-02-01' }
  ];
  const name = (i: { name: string }) => i.name;
  const date = (i: { date: string }) => i.date;

  it('ordena por nombre sin distinguir mayúsculas', () => {
    expect(sortByKey(items, { key: 'name', dir: 'asc' }, name, date).map(name)).toEqual(['Alfa', 'beta', 'gamma']);
    expect(sortByKey(items, { key: 'name', dir: 'desc' }, name, date).map(name)).toEqual(['gamma', 'beta', 'Alfa']);
  });

  it('ordena por fecha', () => {
    expect(sortByKey(items, { key: 'date', dir: 'desc' }, name, date).map(name)).toEqual(['Alfa', 'gamma', 'beta']);
  });
});
