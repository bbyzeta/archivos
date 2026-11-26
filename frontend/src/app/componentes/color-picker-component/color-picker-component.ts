import { Component, ElementRef, HostListener, inject, input, output, signal } from '@angular/core';
import { DEFAULT_FOLDER_COLOR, FOLDER_COLORS } from '../../utils/folder-colors';

/** Botón con paleta para elegir el color de una carpeta (o quitarlo). */
@Component({
  selector: 'app-color-picker',
  standalone: true,
  templateUrl: './color-picker-component.html',
  styleUrl: './color-picker-component.scss'
})
export class ColorPickerComponent {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Color actual (hex) o null. */
  readonly current = input<string | null>(null);
  /** Emite el color elegido, o null para quitarlo. */
  readonly pick = output<string | null>();

  readonly colors = FOLDER_COLORS;
  readonly defaultColor = DEFAULT_FOLDER_COLOR;
  readonly open = signal(false);
  /** Posición fija del panel: evita que lo recorte un contenedor con scroll (tablas). */
  readonly position = signal({ top: 0, left: 0 });

  toggle(event: MouseEvent): void {
    event.stopPropagation();
    if (this.open()) {
      this.open.set(false);
      return;
    }
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const panelWidth = 200;
    const left = Math.max(8, Math.min(rect.right - panelWidth, window.innerWidth - panelWidth - 8));
    this.position.set({ top: rect.bottom + 6, left });
    this.open.set(true);
  }

  select(color: string | null): void {
    this.pick.emit(color);
    this.open.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) this.open.set(false);
  }

  @HostListener('document:keydown.escape')
  @HostListener('window:resize')
  @HostListener('window:scroll')
  close(): void {
    this.open.set(false);
  }
}
