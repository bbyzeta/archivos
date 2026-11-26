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

type ModalMode = 'create' | 'edit' | 'reset' | null;
type SortKey = 'username' | 'fullName' | 'role' | 'active' | 'createdAt';
type RoleFilter = 'all' | UserRole;
type StatusFilter = 'all' | 'active' | 'inactive';

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

  // Búsqueda, filtros y orden
  readonly search = signal('');
  readonly roleFilter = signal<RoleFilter>('all');
  readonly statusFilter = signal<StatusFilter>('all');
  readonly sortKey = signal<SortKey>('createdAt');
  readonly sortDir = signal<'asc' | 'desc'>('asc');

  readonly currentUserId = computed(() => this.auth.currentUser()?.userId ?? '');

  readonly visibleUsers = computed(() => {
    const term = this.normalize(this.search());
    const role = this.roleFilter();
    const status = this.statusFilter();
    const key = this.sortKey();
    const sign = this.sortDir() === 'asc' ? 1 : -1;

    return this.users()
      .filter((u) => (role === 'all' || u.role === role)
        && (status === 'all' || (status === 'active') === u.active)
        && (!term || this.normalize(`${u.username} ${u.fullName}`).includes(term)))
      .sort((a, b) => this.compare(a, b, key) * sign || this.compare(a, b, 'username'));
  });

  readonly summary = computed(() => {
    const total = this.users().length;
    const active = this.users().filter((u) => u.active).length;
    const shown = this.visibleUsers().length;
    const base = `${total} ${total === 1 ? 'usuario' : 'usuarios'} · ${active} ${active === 1 ? 'activo' : 'activos'}`;
    return shown === total ? base : `${shown} de ${base}`;
  });

  readonly hasFilters = computed(() =>
    !!this.search().trim() || this.roleFilter() !== 'all' || this.statusFilter() !== 'all');

  readonly createForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9._@-]{3,100}$/)]],
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    role: ['USER' as UserRole, [Validators.required]]
  });

  readonly editForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    role: ['USER' as UserRole, [Validators.required]]
  });

  readonly resetForm = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]]
  });

  constructor() {
    this.loadUsers();
  }

  // ---------- búsqueda, filtros y orden ----------

  onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  onRoleFilter(event: Event): void {
    this.roleFilter.set((event.target as HTMLSelectElement).value as RoleFilter);
  }

  onStatusFilter(event: Event): void {
    this.statusFilter.set((event.target as HTMLSelectElement).value as StatusFilter);
  }

  clearFilters(): void {
    this.search.set('');
    this.roleFilter.set('all');
    this.statusFilter.set('all');
  }

  /** Clic en un encabezado: ordena por esa columna o invierte el sentido si ya estaba activa. */
  sortBy(key: SortKey): void {
    if (this.sortKey() === key) {
      this.sortDir.update((dir) => (dir === 'asc' ? 'desc' : 'asc'));
    } else {
      this.sortKey.set(key);
      this.sortDir.set('asc');
    }
  }

  sortIcon(key: SortKey): string {
    if (this.sortKey() !== key) return 'bi-arrow-down-up';
    return this.sortDir() === 'asc' ? 'bi-sort-up' : 'bi-sort-down';
  }

  ariaSort(key: SortKey): 'ascending' | 'descending' | 'none' {
    if (this.sortKey() !== key) return 'none';
    return this.sortDir() === 'asc' ? 'ascending' : 'descending';
  }

  // ---------- modales ----------

  openCreate(): void {
    this.createForm.reset({ role: 'USER' });
    this.selected.set(null);
    this.modal.set('create');
  }

  openEdit(user: User): void {
    this.editForm.reset({ fullName: user.fullName, role: user.role });
    this.selected.set(user);
    this.modal.set('edit');
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

  invalid(form: 'create' | 'edit' | 'reset', control: string): boolean {
    const group: FormGroup = form === 'create' ? this.createForm : form === 'edit' ? this.editForm : this.resetForm;
    const c = group.get(control);
    return !!c && c.invalid && (c.dirty || c.touched);
  }

  /** Nadie puede cambiar su propio rol (el backend también lo impide). */
  isSelf(user: User | null): boolean {
    return !!user && user.id === this.currentUserId();
  }

  // ---------- acciones ----------

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

  updateUser(): void {
    const user = this.selected();
    if (!user || this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const { fullName, role } = this.editForm.getRawValue();
    this.saving.set(true);
    this.userService.update(user.id, { fullName: fullName.trim(), role }).subscribe({
      next: (updated) => {
        this.replaceUser(updated);
        this.saving.set(false);
        this.modal.set(null);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.alertService.mostrarAlertaError(this.errorMessage(err, 'No se pudo actualizar el usuario.'));
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
        this.alertService.mostrarAlertaConTimer(
          'Listo', `Se restableció la contraseña de ${user.username}. Sus sesiones abiertas se cerrarán.`);
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
      next: (updated) => this.replaceUser(updated),
      error: (err: HttpErrorResponse) => this.alertService.mostrarAlertaError(
        this.errorMessage(err, 'No se pudo actualizar el usuario.'))
    });
  }

  // ---------- internos ----------

  private replaceUser(updated: User): void {
    this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
  }

  /** Mensajes de las reglas de negocio del backend (400 propia cuenta/rol, 409 último administrador). */
  private errorMessage(err: HttpErrorResponse, fallback: string): string {
    const detail: string | undefined = err.error?.message ?? err.error?.detail;
    return (err.status === 400 || err.status === 409) && detail ? detail : fallback;
  }

  private compare(a: User, b: User, key: SortKey): number {
    switch (key) {
      case 'active': return Number(a.active) - Number(b.active);
      case 'createdAt': return a.createdAt.localeCompare(b.createdAt);
      default: return a[key].localeCompare(b[key], 'es', { sensitivity: 'base' });
    }
  }

  private normalize(text: string): string {
    return text.trim().toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
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
