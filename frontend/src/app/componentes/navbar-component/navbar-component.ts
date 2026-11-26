import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

/** Barra superior compartida por las vistas protegidas. */
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink],
  template: `
    <nav class="navbar bg-light border-bottom px-3">
      <a class="navbar-brand mb-0 h1" routerLink="/home">Gestor de Archivos</a>
      <div class="d-flex align-items-center gap-3">
        <span class="text-muted small">{{ auth.currentUser()?.fullName }}</span>
        <button type="button" class="btn btn-outline-secondary btn-sm" (click)="auth.logout()">
          <i class="bi bi-box-arrow-right me-1"></i>Cerrar sesión
        </button>
      </div>
    </nav>
  `
})
export class NavbarComponent {
  protected readonly auth = inject(AuthService);
}
