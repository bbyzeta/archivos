import { Component, input, output } from '@angular/core';
import { ViewMode } from '../../services/explorer-preferences.service';
import { SortKey, SortState } from '../../utils/file-explorer';

const LABELS: Record<SortKey, string> = { name: 'Nombre', date: 'Fecha', size: 'Tamaño', type: 'Tipo' };

/** Barra del explorador: contador de elementos, orden (campo y sentido) y vista cuadrícula/lista. */
@Component({
  selector: 'app-explorer-toolbar',
  standalone: true,
  templateUrl: './explorer-toolbar-component.html',
  styleUrl: './explorer-toolbar-component.scss'
})
export class ExplorerToolbarComponent {
  /** Texto resumen, p. ej. "3 carpetas · 12 archivos". */
  readonly summary = input('');
  readonly view = input.required<ViewMode>();
  readonly sort = input.required<SortState>();
  /** Campos por los que se puede ordenar. */
  readonly keys = input<SortKey[]>(['name', 'date', 'size', 'type']);

  readonly viewChange = output<ViewMode>();
  readonly sortChange = output<SortState>();

  protected readonly labels = LABELS;

  onKeyChange(event: Event): void {
    this.sortChange.emit({ ...this.sort(), key: (event.target as HTMLSelectElement).value as SortKey });
  }

  toggleDirection(): void {
    this.sortChange.emit({ ...this.sort(), dir: this.sort().dir === 'asc' ? 'desc' : 'asc' });
  }
}
