import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

import { Estado } from '../../models/estado.model';
import { EstadoService } from '../../services/estado.service';
import { ParticipanteService } from '../../services/participante.service';
import { Observable } from 'rxjs';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import {  AlertaService } from '../alerts/alert-component';
import { listaTitulos } from '../../models/participante.model';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registro-component.html',
  styleUrls: ['./registro-component.scss']
})
export class RegistroComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly estadoService = inject(EstadoService);
  private readonly participanteService = inject(ParticipanteService);
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly alerta = inject(AlertaService);

  registroForm!: FormGroup;
  estados$: Observable<Estado[]> = this.estadoService.getEstados();
  enviando: boolean = false;
  mensajeError: string | null = null;
  mostrarModalPdf: boolean = false;
  pdfUrlSanitizada: SafeResourceUrl | null = null;
  anioActual: number = new Date().getFullYear();
  titulos = listaTitulos;
  
  constructor() {
    this.inicializarFormulario();
  }

  ngOnInit(): void {}

  private inicializarFormulario(): void {
    this.registroForm = this.fb.group({
      nombres: ['', [Validators.required, Validators.maxLength(100)]],
      apellidos: ['', [Validators.required, Validators.maxLength(150)]],
      curp: ['', [
        Validators.required, 
        Validators.pattern('^[A-Z]{4}\\d{6}[HM][A-Z]{5}[0-9A-Z]\\d$')
      ]],
      telefono: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      email: ['', [Validators.required, Validators.email]],
      confirmEmail: ['', [Validators.required]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&._-]).{8,}$/)
      ]],
      confirmPassword: ['', [Validators.required]],
      //idEstado: ['', [Validators.required]],
      //institucion: ['', [Validators.required]],
      titulo: ['', [Validators.required]],
      aceptoTerminos: [false, [Validators.requiredTrue]]
    }, { 
      validators: [
        this.validarCoincidencia('email', 'confirmEmail', 'emailsNoCoinciden'),
        this.validarCoincidencia('password', 'confirmPassword', 'passwordsNoCoinciden')
      ]
    });
  }

  private validarCoincidencia(controlName: string, matchingControlName: string, errorKey: string) {
    return (group: AbstractControl): ValidationErrors | null => {
      const control = group.get(controlName);
      const matchingControl = group.get(matchingControlName);

      if (!control || !matchingControl) return null;

      if (matchingControl.errors && !matchingControl.errors[errorKey]) {
        return null;
      }

      if (control.value !== matchingControl.value) {
        matchingControl.setErrors({ [errorKey]: true });
      } else {
        matchingControl.setErrors(null);
      }
      return null;
    };
  }

  async onSubmit(): Promise<void> {
    this.mensajeError = null;

    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      return;
    }

    const acepto = await this.alerta.confirmarAccion(
      '¿Confirmar registro?',
      '¿Estás seguro de que los datos ingresados son correctos?',
      'Sí, registrar',
      'Cancelar'
    );

    if (!acepto) return;

    this.alerta.mostrarLoader();

    const { confirmEmail, confirmPassword, ...datosAEnviar } = this.registroForm.value;

    this.participanteService.registrar(datosAEnviar).subscribe({
      next: () => {
        this.alerta.mostrarAlerta('¡Registro exitoso!','Redirigiendo al inicio de sesión.','success','Aceptar','#13322b');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.alerta.cerrarLoader();
        this.mensajeError = err.error?.error || 'Ocurrió un error al registrar el usuario.';
        this.alerta.mostrarAlerta('!Atención¡',err.error?.error || 'Ocurrió un error al registrar el usuario.','error','Aceptar','#9b2247');
      }
    });
  }

  esCampoInvalido(campo: string): boolean {
    const control = this.registroForm.get(campo);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  abrirAvisoPrivacidad(event: Event): void {
    event.preventDefault(); 
    
    const rutaPdf = 'assets/docs/aviso_privacidad.pdf';
    
    this.pdfUrlSanitizada = this.sanitizer.bypassSecurityTrustResourceUrl(rutaPdf);
    this.mostrarModalPdf = true;
  }

  cerrarModalPdf(): void {
    this.mostrarModalPdf = false;
  }

}