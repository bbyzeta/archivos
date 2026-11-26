import { TestBed } from '@angular/core/testing';
import { ExplorerPreferencesService } from './explorer-preferences.service';

const KEY = 'explorer.prefs';

function create(): ExplorerPreferencesService {
  TestBed.resetTestingModule();
  return TestBed.inject(ExplorerPreferencesService);
}

describe('ExplorerPreferencesService', () => {
  beforeEach(() => localStorage.removeItem(KEY));
  afterEach(() => localStorage.removeItem(KEY));

  it('usa cuadrícula y orden por nombre por defecto', () => {
    const prefs = create();

    expect(prefs.view()).toBe('grid');
    expect(prefs.homeSort()).toEqual({ key: 'name', dir: 'asc' });
    expect(prefs.folderSort()).toEqual({ key: 'name', dir: 'asc' });
  });

  it('persiste la vista y el orden entre sesiones', () => {
    const first = create();
    first.setView('list');
    first.setFolderSort({ key: 'size', dir: 'desc' });
    first.setHomeSort({ key: 'date', dir: 'desc' });

    const second = create();

    expect(second.view()).toBe('list');
    expect(second.folderSort()).toEqual({ key: 'size', dir: 'desc' });
    expect(second.homeSort()).toEqual({ key: 'date', dir: 'desc' });
  });

  it('ignora valores guardados corruptos o no válidos', () => {
    localStorage.setItem(KEY, JSON.stringify({ view: 'tabla', homeSort: { key: 'peso', dir: 'x' }, folderSort: 5 }));

    const prefs = create();

    expect(prefs.view()).toBe('grid');
    expect(prefs.homeSort()).toEqual({ key: 'name', dir: 'asc' });
    expect(prefs.folderSort()).toEqual({ key: 'name', dir: 'asc' });
  });

  it('un JSON roto no impide arrancar', () => {
    localStorage.setItem(KEY, '{no es json');

    expect(create().view()).toBe('grid');
  });
});
