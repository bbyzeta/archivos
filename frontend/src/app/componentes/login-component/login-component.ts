import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NgClass } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { AlertaService } from '../alerts/alert-component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule],
  templateUrl: './login-component.html',
  styleUrls: ['./login-component.scss']
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly alertService = inject(AlertaService);
  private readonly router = inject(Router);

  /** Envío en curso: bloquea el botón y evita enviar dos veces. */
  readonly submitting = signal(false);
  readonly showPassword = signal(false);

  readonly loginForm = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  toggleShowPassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  onSubmit(): void {
    if (this.submitting()) return;

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.alertService.mostrarLoader('Iniciando sesión...');

    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: () => {
        this.alertService.cerrarLoader();
        this.submitting.set(false);
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.alertService.cerrarLoader();
        this.submitting.set(false);
        const msg = err.status === 0
          ? 'No se pudo conectar con el servidor.'
          : 'Usuario o contraseña incorrectos.';
        this.alertService.mostrarAlerta('¡Atención!', msg, 'warning', 'Aceptar', '#13322b');
      }
    });
  }

  esCampoInvalido(campo: 'username' | 'password'): boolean {
    const control = this.loginForm.controls[campo];
    return control.invalid && (control.dirty || control.touched);
  }
}
