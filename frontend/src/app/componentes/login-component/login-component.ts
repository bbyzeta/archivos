import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { AlertaService } from '../alerts/alert-component';


@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login-component.html',
  styleUrls: ['./login-component.scss']
})
export class LoginComponent {

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly alertService = inject(AlertaService);
  private readonly router = inject(Router);

  loginForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.alertService.mostrarLoader('Iniciando sesión...');

    const credentials = this.loginForm.value;

    this.authService.login(credentials).subscribe({
      next: (res) => {
       
        this.alertService.cerrarLoader();
        
        
        switch(res.rol){
          case 'Administrador':
            this.router.navigate(['/convocatoria-admin']);
            break;
          case 'Auditor':
            this.router.navigate(['/auditor/postulantes']);
            break;
          case 'Jurado':
            this.router.navigate(['/jurado/postulantes']);
            break;
          case 'Postulante':
            this.router.navigate(['/postulante/convocatorias']);
            break;
          default:
            this.router.navigate(['/login']);
        }
      },
      error: (err) => {
        this.alertService.cerrarLoader();
        const msgError = err.status === 401 
          ? 'Este swervicio no esta autorizado' 
          : 'Correo o contraseña incorrectos.';

        this.alertService.mostrarAlerta('!Atención¡', msgError, 'warning', 'Aceptar', '#13322b');
      }
    });
  }

  esCampoInvalido(campo: string): boolean {
    const control = this.loginForm.get(campo);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}