import { Component, inject } from '@angular/core';
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

  readonly loginForm = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.alertService.mostrarLoader('Iniciando sesión...');

    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: () => {
        this.alertService.cerrarLoader();
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.alertService.cerrarLoader();
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
