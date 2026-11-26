import { Component, computed, input, output } from '@angular/core';
import { breadcrumbs, parentPath } from '../../utils/file-explorer';

/** Ruta actual del explorador con botón para subir de nivel. Compartido por la vista admin y la pública. */
@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  template: `
    <nav aria-label="Ruta actual" class="d-flex align-items-center gap-2 flex-wrap mb-3">
      <button type="button" class="btn btn-outline-secondary btn-sm" title="Subir un nivel"
              [disabled]="!path()" (click)="navigate.emit(parent())">
        <i class="bi bi-arrow-up"></i>
      </button>
      <ol class="breadcrumb mb-0">
        @if (crumbs().length === 0) {
          <li class="breadcrumb-item active" aria-current="page">{{ rootLabel() }}</li>
        } @else {
          <li class="breadcrumb-item">
            <a href="#" class="text-decoration-none" (click)="$event.preventDefault(); navigate.emit('')">{{ rootLabel() }}</a>
          </li>
          @for (crumb of crumbs(); track crumb.path; let last = $last) {
            @if (last) {
              <li class="breadcrumb-item active text-break" aria-current="page">{{ crumb.name }}</li>
            } @else {
              <li class="breadcrumb-item">
                <a href="#" class="text-decoration-none text-break"
                   (click)="$event.preventDefault(); navigate.emit(crumb.path)">{{ crumb.name }}</a>
              </li>
            }
          }
        }
      </ol>
    </nav>
  `
})
export class BreadcrumbComponent {
  readonly path = input.required<string>();
  readonly rootLabel = input('Inicio');
  readonly navigate = output<string>();

  protected readonly crumbs = computed(() => breadcrumbs(this.path()));
  protected readonly parent = computed(() => parentPath(this.path()));
}
