import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { RecuperarService } from '../../services/recuperar.service';

@Component({
  selector: 'app-recuperar-pass-word-component',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './recuperar-pass-word-component.html',
  styleUrl: './recuperar-pass-word-component.scss',
})
export class RecuperarPasswordComponent implements OnInit {
  private readonly recuperarService = inject(RecuperarService);
  seccion = signal<number>(0);
  
  token: string | null = null;
  cargando = signal<boolean>(false);
  mensajeExito = signal<string | null>(null);
  mensajeError = signal<string | null>(null);

  solicitarForm!: FormGroup;
  cambiarPasswordForm!: FormGroup;

  tipoInput = signal<string>("password");
  tipoInputConfirm = signal<string>("password");

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.solicitarForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });

    this.cambiarPasswordForm = this.fb.group({
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&._-]).{8,}$/)
      ]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.coincidenPasswordsValidator });

    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || null;
      if (this.token) {
        this.seccion.set(1);
      } else {
        this.seccion.set(0);
      }
    });
  }

  private coincidenPasswordsValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { noCoinciden: true };
  }

  titulo(): string {
    return this.seccion() === 0 ? 'Recuperar Contraseña' : 'Nueva Contraseña';
  }

  onSubmitSolicitar(): void {
    if (this.solicitarForm.invalid) {
      this.solicitarForm.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.mensajeError.set(null);
    const email = this.solicitarForm.value.email;

     this.recuperarService.recuperarCuenta(email).subscribe({
      next: (data) =>{
        console.log(data);

        setTimeout(() => {
          this.cargando.set(false);
          this.mensajeExito.set(
            'Si el correo electrónico existe en nuestro sistema, recibirá un mensaje con las instrucciones y el enlace para restablecer su contraseña.'
          );
          this.solicitarForm.reset();
        }, 1500);
       
      },
      error: (err) =>{
        console.log(err);
      }
      
    });

    
  }

  onSubmitCambiar(): void {
    if (this.cambiarPasswordForm.invalid) {
      this.cambiarPasswordForm.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.mensajeError.set(null);
    const nuevaPassword = this.cambiarPasswordForm.value.password;

    this.recuperarService.restablecerCuenta(nuevaPassword, this.token!).subscribe({
        next: (data)=>{
          

          setTimeout(() => {
            this.cargando.set(false);
            this.mensajeExito.set('Su contraseña ha sido actualizada exitosamente.');
            setTimeout(() => this.router.navigate(['/login']), 2500);
          }, 1500);
        },
        error: (err) =>{
          console.log(err);
          setTimeout(() => {
            this.cargando.set(false);
            this.mensajeError.set('El token no es válido, favor de solicitar uno nuevo.');
            this.mensajeExito.set('');
          }, 1500);
        }
    });

    
  }

  togglePassword():void{
   this.tipoInput() === "password" ? this.tipoInput.set("text") : this.tipoInput.set("password");
  }
  
  togglePasswordConfirm():void{
   this.tipoInputConfirm() === "password" ? this.tipoInputConfirm.set("text") : this.tipoInputConfirm.set("password");
  }
}