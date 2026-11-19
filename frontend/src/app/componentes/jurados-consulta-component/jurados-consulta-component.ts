import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { JuradoItem } from '../../models/jurado.models';
import { JuradoService } from '../../services/jurado.service';
import { AlertaService } from '../alerts/alert-component';

declare const bootstrap: any;

@Component({
  selector: 'app-jurados-consulta',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './jurados-consulta-component.html',
  styleUrls: ['./jurados-consulta-component.scss']
})
export class JuradosConsultaComponent implements OnInit {
  private readonly juradoService = inject(JuradoService);
  private readonly alertService = inject(AlertaService);
  private readonly fb = inject(FormBuilder);

  cargando = signal<boolean>(false);
  terminoBusqueda = signal<string>('');
  jurados = signal<JuradoItem[]>([]);
  paginaActual = signal<number>(0);
  totalPaginas = signal<number>(1);
  totalElementos = signal<number>(0);
  tamanioPagina = signal<number>(10);

  esModoEdicion = signal<boolean>(false);
  mostrarPassword = signal<boolean>(false);
  juradoSeleccionado = signal<JuradoItem | null>(null);

  formJurado!: FormGroup;

  private readonly passPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&._-]).{8,}$/;

  ngOnInit(): void {
    this.iniciarFormulario();
    this.cargarJurados();
  }

  iniciarFormulario(): void {
    this.formJurado = this.fb.group({
      idJurado: [null],
      gradoAcademico: ['', Validators.required],
      nombres: ['', Validators.required],
      apellidos: ['', Validators.required],
      rfc: [''],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      username: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.pattern(this.passPattern)]],
      institucionAdscripcion: ['', Validators.required],
      cargoActual: ['', Validators.required],
      areaConocimiento: ['', Validators.required],
      orcidUrl: [''],
      lineasInvestigacion: ['', Validators.required],
      aceptoConflictoInteres: [false, Validators.requiredTrue],
      aceptoConfidencialidad: [false, Validators.requiredTrue],
      activo: [true]
    });
  }

  cargarJurados(): void {
    this.cargando.set(true);
    this.juradoService.listarPaginado(this.paginaActual(), this.tamanioPagina(), this.terminoBusqueda())
      .subscribe({
        next: (resp: any) => {
          this.jurados.set(resp.content || []);
          this.totalPaginas.set(resp.totalPages || 1);
          this.totalElementos.set(resp.totalElements || 0);
          this.cargando.set(false);
        },
        error: (err: any) => {
          console.error('Error al cargar jurados:', err);
          this.cargando.set(false);
          this.alertService.mostrarAlertaError('No se pudo cargar el listado de jurados.');
        }
      });
  }

  buscar(): void {
    this.paginaActual.set(0);
    this.cargarJurados();
  }

  limpiarBusqueda(): void {
    this.terminoBusqueda.set('');
    this.buscar();
  }

  cambiarPagina(nuevaPagina: number): void {
    if (nuevaPagina >= 0 && nuevaPagina < this.totalPaginas() && nuevaPagina !== this.paginaActual()) {
      this.paginaActual.set(nuevaPagina);
      this.cargarJurados();
    }
  }

  obtenerArregloPaginas(): number[] {
    return Array.from({ length: this.totalPaginas() }, (_, i) => i);
  }

  campoInvalido(campo: string): boolean {
    const control = this.formJurado.get(campo);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  abrirCrear(): void {
    this.esModoEdicion.set(false);
    this.mostrarPassword.set(false);
    this.formJurado.reset({
      gradoAcademico: '',
      areaConocimiento: '',
      aceptoConflictoInteres: false,
      aceptoConfidencialidad: false,
      activo: true
    });

    const passControl = this.formJurado.get('password');
    passControl?.setValidators([Validators.required, Validators.pattern(this.passPattern)]);
    passControl?.updateValueAndValidity();

    const modalEl = document.getElementById('modalFormJurado');
    if (modalEl) {
      const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
      modal.show();
    }
  }

  abrirEditar(jurado: JuradoItem): void {
    this.esModoEdicion.set(true);
    this.mostrarPassword.set(false);
    this.formJurado.patchValue(jurado);

    const passControl = this.formJurado.get('password');
    passControl?.clearValidators();
    passControl?.setValidators([Validators.pattern(this.passPattern)]);
    passControl?.updateValueAndValidity();

    const modalEl = document.getElementById('modalFormJurado');
    if (modalEl) {
      const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
      modal.show();
    }
  }

  abrirDetalle(item: JuradoItem): void {
    this.juradoSeleccionado.set(item);
    const modalEl = document.getElementById('modalDetalleJurado');
    if (modalEl) {
      const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
      modal.show();
    }
  }

  guardar(): void {
    if (this.formJurado.invalid) {
      this.formJurado.markAllAsTouched();
      this.alertService.mostrarAlerta(
        'Formulario incompleto',
        'Por favor completa los campos obligatorios (*) y confirma los acuerdos éticos.',
        'warning',
        'Aceptar',
        '#184B35'
      );
      return;
    }

    const val = this.formJurado.value;
    const request: Partial<JuradoItem> = {
      ...val,
      nombres: val.nombres?.trim(),
      apellidos: val.apellidos?.trim(),
      rfc: val.rfc ? val.rfc.trim().toUpperCase() : undefined,
      username: val.username?.trim().toLowerCase(),
      password: val.password || undefined
    };

    const modalEl = document.getElementById('modalFormJurado');
    const modal = modalEl ? bootstrap.Modal.getInstance(modalEl) : null;

    if (this.esModoEdicion()) {
      this.alertService.mostrarLoader('Guardando cambios del evaluador...');
      this.juradoService.actualizar(val.idJurado, request).subscribe({
        next: () => {
          this.alertService.cerrarLoader();
          modal?.hide();
          this.alertService.mostrarAlertaConTimer(
            '¡Actualizado!',
            'Los datos del jurado se actualizaron correctamente.',
            1800,
            'success'
          );
          this.cargarJurados();
        },
        error: (err: any) => {
          console.error(err);
          this.alertService.cerrarLoader();
          const mensaje = err.error?.message || 'No se pudo actualizar el evaluador.';
          this.alertService.mostrarAlertaError(mensaje);
        }
      });
    } else {
      this.alertService.mostrarLoader('Registrando jurado...');
      this.juradoService.crear(request).subscribe({
        next: () => {
          this.alertService.cerrarLoader();
          modal?.hide();
          this.alertService.mostrarAlertaConTimer(
            '¡Creado!',
            'El jurado dictaminador fue registrado exitosamente.',
            1800,
            'success'
          );
          this.cargarJurados();
        },
        error: (err: any) => {
          console.error(err);
          this.alertService.cerrarLoader();
          const mensaje = err.error?.message || 'No se pudo registrar el evaluador.';
          this.alertService.mostrarAlertaError(mensaje);
        }
      });
    }
  }

  async toggleActivo(event: Event, item: JuradoItem): Promise<void> {
    event.preventDefault();

    const accion = item.activo ? 'desactivar' : 'activar';

    const confirmado = await this.alertService.confirmarAccion(
      `¿Deseas ${accion} al jurado?`,
      `El evaluador "${item.gradoAcademico} ${item.nombres} ${item.apellidos}" ${item.activo ? 'no podrá dictaminar convocatorias' : 'estará disponible para asignación'}.`,
      `Sí, ${accion}`,
      'Cancelar'
    );

    if (!confirmado) {
      return;
    }

    this.alertService.mostrarLoader(`${item.activo ? 'Desactivando' : 'Activando'} evaluador...`);

    this.juradoService.cambiarEstado(item.idJurado, !item.activo).subscribe({
      next: () => {
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaConTimer(
          '¡Estado actualizado!',
          `El jurado ha sido ${item.activo ? 'desactivado' : 'activado'}.`,
          1800,
          'success'
        );
        this.cargarJurados();
      },
      error: (err: any) => {
        console.error(err);
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaError('Error al cambiar el estado del jurado.');
      }
    });
  }

  async prepararEliminar(item: JuradoItem): Promise<void> {
    const confirmado = await this.alertService.confirmarAccion(
      '¿Eliminar jurado?',
      `Se eliminará el expediente de "${item.gradoAcademico} ${item.nombres} ${item.apellidos}" y su acceso institucional.`,
      'Sí, eliminar',
      'Cancelar'
    );

    if (confirmado) {
      this.confirmarEliminacion(item.idJurado);
    }
  }

  private confirmarEliminacion(id: number): void {
    this.alertService.mostrarLoader('Eliminando jurado...');
    this.juradoService.eliminar(id).subscribe({
      next: () => {
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaConTimer(
          '¡Eliminado!',
          'El jurado ha sido eliminado del sistema.',
          2000,
          'success'
        );
        this.cargarJurados();
      },
      error: (err: any) => {
        console.error(err);
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaError('Ocurrió un error al eliminar el evaluador.');
      }
    });
  }

  obtenerEtiquetaArea(codigo: string): string {
    const areas: Record<string, string> = {
      'MEDICINA_CLINICA': 'Ciencias Médicas y Clínicas',
      'SALUD_PUBLICA': 'Salud Pública y Epidemiología',
      'BIOTECNOLOGIA': 'Biotecnología y Biomedicina',
      'INNOVACION_TI': 'Tecnologías de la Información en Salud',
      'FARMACOLOGIA': 'Farmacología y Terapéutica',
      'CIENCIAS_SOCIALES': 'Gestión y Políticas de Salud'
    };
    return areas[codigo] || codigo || 'No asignada';
  }
}