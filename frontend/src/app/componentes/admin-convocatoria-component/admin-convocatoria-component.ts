import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AlertaService } from '../alerts/alert-component';
import { AdminService } from '../../services/admin.service';
import { Router } from '@angular/router';
import { ConsultaConvocatoriasComponent } from '../consulta-convocatorias-component/consulta-convocatorias-component';
import { ConvocatoriaResponseDTO } from '../../models/convocatorias.model';
import { ConsultaParticipantesComponent } from '../consulta-participantes-component/consulta-participantes-component';
import { JuradosConsultaComponent } from '../jurados-consulta-component/jurados-consulta-component';
import { DictamenesListComponent } from '../dictamenes-list-component/dictamenes-list-component';

@Component({
  selector: 'app-admin-convocatoria',
  standalone: true,
  imports: [CommonModule, 
    ReactiveFormsModule, 
    ConsultaConvocatoriasComponent, 
    ConsultaParticipantesComponent, 
    JuradosConsultaComponent,
    DictamenesListComponent],
  templateUrl: './admin-convocatoria-component.html',
  styleUrls: ['./admin-convocatoria-component.scss']
})
export class AdminConvocatoriaComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly alertService = inject(AlertaService);
  private readonly adminService = inject(AdminService);
  private readonly router = inject(Router);

  opcionSidebar: 'nueva' | 'consulta' | 'usuarios' | 'jurado' | 'evaluacion' = 'nueva';
  imagenSeleccionada: File | null = null;
  documentoBasesSeleccionado: File | null = null;
  anioActual: number = new Date().getFullYear();
  
  usuario = "";
  rol = "";
  convocatoriaEnEdicionId: number | null = null;

  ngOnInit(): void {
    this.usuario = sessionStorage.getItem('userName')?.replaceAll("\"", "") || "";
    this.rol = sessionStorage.getItem('rol')?.replaceAll("\"", "") || "";
  }

  convocatoriaForm: FormGroup = this.fb.group({
    titulo: ['', [Validators.required]],
    subtitulo: [''],
    tituloBlog: ['', [Validators.required]],
    fechaApertura: ['', [Validators.required]],
    fechaCierre: ['', [Validators.required]],
    contenidoBlog: ['', [Validators.required]],
    imagenHeader: [null, [Validators.required]], 
    documentoBases: [null, [Validators.required]],
    activa: [false], 
    requisitos: this.fb.array([], [Validators.required]),
    ganadoresHistoricos: this.fb.array([]),
    areaGrupoPremio: ['', [Validators.required]],
  });

  get requisitos(): FormArray {
    return this.convocatoriaForm.get('requisitos') as FormArray;
  }

  get ganadoresHistoricos(): FormArray {
    return this.convocatoriaForm.get('ganadoresHistoricos') as FormArray;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.imagenSeleccionada = input.files[0];
      this.convocatoriaForm.patchValue({ imagenHeader: this.imagenSeleccionada });
      this.convocatoriaForm.get('imagenHeader')?.updateValueAndValidity();
    }
  }

  onBasesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (input.files && input.files.length > 0) {
      const archivo = input.files[0];

      if (archivo.type !== 'application/pdf' && !archivo.name.toLowerCase().endsWith('.pdf')) {
        this.alertService.mostrarAlerta(
          'Archivo no válido', 
          'El documento de bases debe ser únicamente en formato PDF (.pdf).', 
          'warning', 
          'Aceptar', 
          '#184B35'
        );
        
        input.value = '';
        this.documentoBasesSeleccionado = null;
        this.convocatoriaForm.patchValue({ documentoBases: null });
        this.convocatoriaForm.get('documentoBases')?.updateValueAndValidity();
        return;
      }

      this.documentoBasesSeleccionado = archivo;
      this.convocatoriaForm.patchValue({ documentoBases: this.documentoBasesSeleccionado });
      this.convocatoriaForm.get('documentoBases')?.updateValueAndValidity();
    }
  }

  agregarRequisito(nombre: string = '', esAnexo: boolean = false, por_min: number = 0, por_max: number = 0): void {
    const reqGroup = this.fb.group({
      nombre: [nombre, Validators.required],
      esAnexo: [esAnexo],
      porcentajeMinimo: [por_min, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern('^[0-9]*$')]],
      porcentajeMaximo: [por_max, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern('^[0-9]*$')]],
    });
    this.requisitos.push(reqGroup);
  }

  eliminarRequisito(index: number): void {
    this.requisitos.removeAt(index);
  }

  agregarGanador(nombre: string = '', anio: number = new Date().getFullYear()): void {
    const ganadorGroup = this.fb.group({
      nombreGanador: [nombre, Validators.required],
      anio: [anio, [Validators.required, Validators.min(1900)]]
    });
    this.ganadoresHistoricos.push(ganadorGroup);
  }

  eliminarGanador(index: number): void {
    this.ganadoresHistoricos.removeAt(index);
  }

  esCampoInvalido(campo: string): boolean {
    const control = this.convocatoriaForm.get(campo);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  cargarEdicion(convocatoria: ConvocatoriaResponseDTO): void {
    this.convocatoriaEnEdicionId = convocatoria.id;
    this.opcionSidebar = 'nueva';

    this.convocatoriaForm.get('imagenHeader')?.clearValidators();
    this.convocatoriaForm.get('documentoBases')?.clearValidators();
    this.convocatoriaForm.get('imagenHeader')?.updateValueAndValidity();
    this.convocatoriaForm.get('documentoBases')?.updateValueAndValidity();

    this.convocatoriaForm.patchValue({
      titulo: convocatoria.titulo,
      subtitulo: convocatoria.subtitulo || '',
      tituloBlog: convocatoria.tituloBlog,
      fechaApertura: convocatoria.fechaApertura,
      fechaCierre: convocatoria.fechaCierre,
      contenidoBlog: convocatoria.contenidoBlog || '',
      activa: convocatoria.activa,
      imagenHeader: null,
      documentoBases: null,
      areaGrupoPremio: convocatoria.idGrupoPremio || ''
    });

    this.requisitos.clear();
    if (convocatoria.requisitos && convocatoria.requisitos.length > 0) {
      convocatoria.requisitos.forEach(r => this.agregarRequisito(r.nombre, r.esAnexo, r.porcentajeMinimo, r.porcentajeMaximo));
    }

    this.ganadoresHistoricos.clear();
    if (convocatoria.ganadoresHistoricos && convocatoria.ganadoresHistoricos.length > 0) {
      convocatoria.ganadoresHistoricos.forEach(g => this.agregarGanador(g.nombreGanador, g.anio));
    }
  }

  limpiarFormulario(): void {
    this.convocatoriaEnEdicionId = null;
    this.requisitos.clear();
    this.ganadoresHistoricos.clear();

      this.convocatoriaForm.reset({
      titulo: '',
      subtitulo: '',
      tituloBlog: '',
      fechaApertura: '',
      fechaCierre: '',
      contenidoBlog: '',
      imagenHeader: null,
      documentoBases: null,
      activa: false,
      areaGrupoPremio: ''
    });

    this.imagenSeleccionada = null;
    this.documentoBasesSeleccionado = null;

    this.convocatoriaForm.get('imagenHeader')?.setValidators([Validators.required]);
    this.convocatoriaForm.get('documentoBases')?.setValidators([Validators.required]);
    this.convocatoriaForm.get('imagenHeader')?.updateValueAndValidity();
    this.convocatoriaForm.get('documentoBases')?.updateValueAndValidity();

    this.convocatoriaForm.markAsPristine();
    this.convocatoriaForm.markAsUntouched();
  }

  async onSubmit(): Promise<void> {
    if (this.convocatoriaForm.invalid) {
      this.convocatoriaForm.markAllAsTouched();
      this.alertService.mostrarAlerta('Formulario incompleto', 'Completa los campos obligatorios (*)', 'warning', 'Aceptar', '#184B35');
      return;
    }

    if (this.requisitos.length === 0) {
      this.alertService.mostrarAlerta('Sin Requisitos', 'Debes agregar al menos un documento o anexo solicitado.', 'warning', 'Aceptar', '#184B35');
      return;
    }

    const estaActiva = this.convocatoriaForm.get('activa')?.value;
    const esEdicion = this.convocatoriaEnEdicionId !== null;
    
    let mensajeConfirmacion = estaActiva
      ? 'Esta convocatoria se guardará como ACTIVA. Si ya existe otra convocatoria abierta en el sistema, pasará automáticamente a estar INACTIVA.'
      : (esEdicion ? '¿Deseas guardar los cambios realizados?' : 'Se registrará el nuevo premio.');

    const confirm = await this.alertService.confirmarAccion(
      esEdicion ? '¿Actualizar Convocatoria?' : '¿Publicar Convocatoria?',
      mensajeConfirmacion,
      'Aceptar'
    );

    if (!confirm) return;

    this.alertService.mostrarLoader(esEdicion ? 'Actualizando Convocatoria...' : 'Guardando Convocatoria...');

    const formData = new FormData();
    formData.append('titulo', this.convocatoriaForm.get('titulo')?.value);
    formData.append('subtitulo', this.convocatoriaForm.get('subtitulo')?.value || '');
    formData.append('tituloBlog', this.convocatoriaForm.get('tituloBlog')?.value);
    formData.append('fechaApertura', this.convocatoriaForm.get('fechaApertura')?.value);
    formData.append('fechaCierre', this.convocatoriaForm.get('fechaCierre')?.value);
    formData.append('contenidoBlog', this.convocatoriaForm.get('contenidoBlog')?.value);
    formData.append('activa', estaActiva ? 'true' : 'false');
    formData.append('idGrupoPremio', this.convocatoriaForm.get('areaGrupoPremio')?.value);
    
    if (this.imagenSeleccionada) {
      formData.append('imagen', this.imagenSeleccionada);
    }

    if (this.documentoBasesSeleccionado) {
      formData.append('documentoBases', this.documentoBasesSeleccionado);
    }
    
    formData.append('requisitos', JSON.stringify(this.requisitos.value));
    formData.append('ganadoresHistoricos', JSON.stringify(this.ganadoresHistoricos.value));

    const peticion = esEdicion
      ? this.adminService.actualizar(this.convocatoriaEnEdicionId!, formData)
      : this.adminService.registrar(formData);

    peticion.subscribe({
      next: () => {
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaConTimer(
          '¡Éxito!',
          esEdicion ? 'Convocatoria actualizada con éxito' : 'Convocatoria registrada con éxito',
          1000,
          'success'
        );
        this.limpiarFormulario();
        this.opcionSidebar = 'consulta';
      },
      error: (e) => {
        console.error(e);
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlerta('Error', 'No se pudo guardar la convocatoria', 'error', 'Aceptar', '#8E1F41');
      }
    });
  }

  async logout() {
    const confirm = await this.alertService.confirmarAccion(
      'Atención',
      '¿Esta seguro de terminar la sesión?',
      'Sí, Salir'
    );

    if (!confirm) return;

    localStorage.clear();
    sessionStorage.clear();
    this.router.navigate(['/login']); 
  }

  cambiarOpcionMenu(opcion: 'nueva' | 'consulta' | 'usuarios' | 'jurado' | 'evaluacion'): void {
    if (opcion === 'nueva') {
      this.limpiarFormulario();
    }
    this.opcionSidebar = opcion;
  }
}