import { Component, HostListener, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { AlertaService } from '../alerts/alert-component';

/** Las dos contraseñas nuevas deben coincidir. */
function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const newPassword = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return newPassword === confirm ? null : { mismatch: true };
}

/** Barra superior compartida por las vistas protegidas. Incluye el cambio de contraseña propia. */
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './navbar-component.html'
})
export class NavbarComponent {
  protected readonly auth = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly alertService = inject(AlertaService);
  private readonly fb = inject(FormBuilder);

  readonly passwordModalOpen = signal(false);
  readonly saving = signal(false);

  readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordsMatch });

  openPasswordModal(): void {
    this.passwordForm.reset();
    this.passwordModalOpen.set(true);
  }

  closePasswordModal(): void {
    if (!this.saving()) this.passwordModalOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.passwordModalOpen()) this.closePasswordModal();
  }

  invalid(control: 'currentPassword' | 'newPassword' | 'confirmPassword'): boolean {
    const c = this.passwordForm.controls[control];
    return c.invalid && (c.dirty || c.touched);
  }

  mismatch(): boolean {
    const confirm = this.passwordForm.controls.confirmPassword;
    return this.passwordForm.hasError('mismatch') && (confirm.dirty || confirm.touched);
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.saving.set(true);
    this.userService.changeOwnPassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.saving.set(false);
        this.passwordModalOpen.set(false);
        // El cambio cierra todas las sesiones abiertas (incluida esta): se vuelve a iniciar sesión
        this.alertService.mostrarAlertaConTimer('Contraseña actualizada', 'Por seguridad, inicia sesión de nuevo.', 2500);
        this.auth.logout();
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.alertService.mostrarAlertaError(err.error?.message ?? err.error?.detail ?? 'No se pudo cambiar la contraseña.');
      }
    });
  }
}
