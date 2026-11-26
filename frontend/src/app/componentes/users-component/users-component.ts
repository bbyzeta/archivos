import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { User, UserRole } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { AlertaService } from '../alerts/alert-component';
import { NavbarComponent } from '../navbar-component/navbar-component';

type ModalMode = 'create' | 'reset' | null;

/** Administración de usuarios (solo rol ADMIN). */
@Component({
  selector: 'app-users',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink, NavbarComponent],
  templateUrl: './users-component.html',
  styleUrl: './users-component.scss'
})
export class UsersComponent {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly alertService = inject(AlertaService);
  private readonly auth = inject(AuthService);

  readonly users = signal<User[]>([]);
  readonly loading = signal(true);
  readonly modal = signal<ModalMode>(null);
  readonly selected = signal<User | null>(null);
  readonly saving = signal(false);

  readonly currentUserId = computed(() => this.auth.currentUser()?.userId ?? '');

  readonly createForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9._@-]{3,100}$/)]],
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    role: ['USER' as UserRole, [Validators.required]]
  });

  readonly resetForm = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]]
  });

  constructor() {
    this.loadUsers();
  }

  openCreate(): void {
    this.createForm.reset({ role: 'USER' });
    this.selected.set(null);
    this.modal.set('create');
  }

  openReset(user: User): void {
    this.resetForm.reset();
    this.selected.set(user);
    this.modal.set('reset');
  }

  closeModal(): void {
    if (!this.saving()) this.modal.set(null);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.modal()) this.closeModal();
  }

  invalid(form: 'create' | 'reset', control: string): boolean {
    const group: FormGroup = form === 'create' ? this.createForm : this.resetForm;
    const c = group.get(control);
    return !!c && c.invalid && (c.dirty || c.touched);
  }

  createUser(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.userService.create(this.createForm.getRawValue()).subscribe({
      next: (user) => {
        this.users.update((list) => [...list, user]);
        this.saving.set(false);
        this.modal.set(null);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.alertService.mostrarAlertaError(err.status === 409
          ? 'Ya existe un usuario con ese nombre.'
          : 'No se pudo crear el usuario. Revisa los datos e intenta de nuevo.');
      }
    });
  }

  resetPassword(): void {
    const user = this.selected();
    if (!user || this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.userService.resetPassword(user.id, this.resetForm.controls.newPassword.value).subscribe({
      next: () => {
        this.saving.set(false);
        this.modal.set(null);
        this.alertService.mostrarAlertaConTimer('Listo', `Se restableció la contraseña de ${user.username}.`);
      },
      error: () => {
        this.saving.set(false);
        this.alertService.mostrarAlertaError('No se pudo restablecer la contraseña.');
      }
    });
  }

  async toggleActive(user: User): Promise<void> {
    const activating = !user.active;
    if (!activating) {
      const confirmed = await this.alertService.confirmarAccion(
        '¿Desactivar usuario?',
        `${user.username} ya no podrá iniciar sesión ni usar su sesión actual.`,
        'Sí, desactivar');
      if (!confirmed) return;
    }

    this.userService.setActive(user.id, activating).subscribe({
      next: (updated) => this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u))),
      error: (err: HttpErrorResponse) => this.alertService.mostrarAlertaError(err.status === 400
        ? 'No puedes desactivar tu propia cuenta.'
        : 'No se pudo actualizar el usuario.')
    });
  }

  private loadUsers(): void {
    this.userService.list().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alertService.mostrarAlertaError('No se pudieron cargar los usuarios.');
      }
    });
  }
}
