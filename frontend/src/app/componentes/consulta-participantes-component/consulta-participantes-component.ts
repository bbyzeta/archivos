import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { ParticipanteService } from '../../services/participante.service';
import { ParticipanteDTO, ParticipanteRequestDTO } from '../../models/participante.model';
import { AlertaService } from '../alerts/alert-component';
import { Estado } from '../../models/estado.model';
import { EstadoService } from '../../services/estado.service';

declare const bootstrap: any;

@Component({
  selector: 'app-consulta-participantes-component',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './consulta-participantes-component.html',
  styleUrls: ['./consulta-participantes-component.scss']
})
export class ConsultaParticipantesComponent implements OnInit {
  private readonly service = inject(ParticipanteService);
  private readonly alertService = inject(AlertaService);
  private readonly fb = inject(FormBuilder);
  private readonly estadoService = inject(EstadoService);

  participantes = signal<ParticipanteDTO[]>([]);
  cargando = signal<boolean>(false);
  terminoBusqueda = signal<string>('');
  mostrarPassword = signal<boolean>(false);

  paginaActual = signal<number>(0);
  tamanioPagina = signal<number>(5);
  totalElementos = signal<number>(0);
  totalPaginas = signal<number>(0);

  participanteSeleccionado = signal<ParticipanteDTO | null>(null);
  esModoEdicion = signal<boolean>(false);

  formParticipante!: FormGroup;
  listaEstados$: Observable<Estado[]> = this.estadoService.getEstados();

  private readonly passPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&._-]).{8,}$/;

  estadoPrecargado: Estado[] = [];

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarDatos();

    this.listaEstados$.subscribe({
      next: (estados) => this.estadoPrecargado = estados,
      error: (err) => console.error('Error al cargar estado', err)
    });
  }

  obtenerNombreEstado(idEstado?: number): string {
    if (!idEstado) return 'No especificado';
    const estadoEncontrado = this.estadoPrecargado.find(e => e.idEstado === idEstado);
    return estadoEncontrado ? estadoEncontrado.nombre : 'Cargando...';
  }

  private inicializarFormulario(): void {
    this.formParticipante = this.fb.group({
      idParticipante: [null],
      nombres: ['', Validators.required],
      apellidos: ['', Validators.required],
      curp: ['', [
        Validators.required, 
        Validators.pattern('^[A-Z]{4}\\d{6}[HM][A-Z]{5}[0-9A-Z]\\d$')
      ]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      username: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.pattern(this.passPattern)]],
      activo: [true],
      datosInstitucionales: this.fb.group({
        idInstitucional: [null],
        idEstado: [null, Validators.required],
        institucion: ['', Validators.required],
        cargo: ['', Validators.required]
      })
    });
  }

  cargarDatos(): void {
    this.cargando.set(true);
    this.service.listar(this.paginaActual(), this.tamanioPagina(), this.terminoBusqueda())
      .subscribe({
        next: (resp) => {
          this.participantes.set(resp.content || []);
          this.totalElementos.set(resp.totalElements || 0);
          this.totalPaginas.set(resp.totalPages || 0);
          this.cargando.set(false);
        },
        error: (err) => {
          console.error('Error al listar participantes', err);
          this.cargando.set(false);
        }
      });
  }

  buscar(): void {
    this.paginaActual.set(0);
    this.cargarDatos();
  }

  limpiarBusqueda(): void {
    this.terminoBusqueda.set('');
    this.buscar();
  }

  cambiarPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPaginas() && pagina !== this.paginaActual()) {
      this.paginaActual.set(pagina);
      this.cargarDatos();
    }
  }

  obtenerArregloPaginas(): number[] {
    return Array.from({ length: this.totalPaginas() }, (_, i) => i);
  }

  abrirCrear(): void {
    this.esModoEdicion.set(false);
    this.mostrarPassword.set(false);
    this.formParticipante.reset({ activo: true });
    
    const passControl = this.formParticipante.get('password');
    passControl?.setValidators([Validators.required, Validators.pattern(this.passPattern)]);
    passControl?.updateValueAndValidity();
    
    const modal = new bootstrap.Modal(document.getElementById('modalFormParticipante'));
    modal.show();
  }

  abrirEditar(item: ParticipanteDTO): void {
    this.esModoEdicion.set(true);
    this.mostrarPassword.set(false);
    
    this.formParticipante.patchValue({
      idParticipante: item.idParticipante,
      nombres: item.nombres,
      apellidos: item.apellidos,
      curp: item.curp,
      telefono: item.telefono,
      username: item.username,
      activo: item.activo,
      datosInstitucionales: {
        idInstitucional: item.datosInstitucionales?.idInstitucional ?? null,
        idEstado: item.datosInstitucionales?.idEstado ?? null,
        institucion: item.datosInstitucionales?.institucion ?? '',
        cargo: item.datosInstitucionales?.cargo ?? ''
      }
    });

    const passControl = this.formParticipante.get('password');
    passControl?.clearValidators();
    passControl?.setValidators([Validators.pattern(this.passPattern)]);
    passControl?.updateValueAndValidity();

    const modal = new bootstrap.Modal(document.getElementById('modalFormParticipante'));
    modal.show();
  }

  abrirDetalle(item: ParticipanteDTO): void {
    this.participanteSeleccionado.set(item);
    const modal = new bootstrap.Modal(document.getElementById('modalDetalleParticipante'));
    modal.show();
  }

  campoInvalido(nombreCampo: string): boolean {
    const control = this.formParticipante.get(nombreCampo);
    return !!(control && control.invalid && (control.touched || control.dirty));
  }

  guardar(): void {
    if (this.formParticipante.invalid) {
      this.formParticipante.markAllAsTouched(); 
      this.alertService.mostrarAlerta('Formulario incompleto', 'Completa los campos obligatorios (*)', 'warning', 'Aceptar', '#184B35');
      return;
    }

    const val = this.formParticipante.value;
    const request: ParticipanteRequestDTO = {
      nombres: val.nombres,
      apellidos: val.apellidos,
      curp: val.curp?.toUpperCase(),
      telefono: val.telefono,
      username: val.username,
      password: val.password || undefined,
      activo: val.activo,
      datosInstitucionales: val.datosInstitucionales
    };

    const modalEl = document.getElementById('modalFormParticipante');
    const modal = bootstrap.Modal.getInstance(modalEl);

    if (this.esModoEdicion()) {
      this.alertService.mostrarLoader('Guardando cambios...');
      this.service.actualizar(val.idParticipante, request).subscribe({
        next: () => {
          this.alertService.cerrarLoader();
          modal?.hide();
          this.alertService.mostrarAlertaConTimer('¡Actualizado!', 'El participante fue actualizado correctamente.', 1800, 'success');
          this.cargarDatos();
        },
        error: (err) => {
          console.error(err);
          this.alertService.cerrarLoader();
          this.alertService.mostrarAlertaError('No se pudo actualizar el registro.');
        }
      });
    } else {
      this.alertService.mostrarLoader('Registrando participante...');
      this.service.crear(request).subscribe({
        next: () => {
          this.alertService.cerrarLoader();
          modal?.hide();
          this.alertService.mostrarAlertaConTimer('¡Creado!', 'El participante fue registrado correctamente.', 1800, 'success');
          this.cargarDatos();
        },
        error: (err) => {
          console.error(err);
          this.alertService.cerrarLoader();
          this.alertService.mostrarAlertaError('No se pudo guardar el registro.');
        }
      });
    }
  }

  async toggleActivo(event: Event, item: ParticipanteDTO): Promise<void> {
    event.preventDefault();

    const accion = item.activo ? 'desactivar' : 'activar';
    
    const confirmado = await this.alertService.confirmarAccion(
      `¿Deseas ${accion} el usuario?`,
      `El participante "${item.nombres} ${item.apellidos}" cambiará su acceso al sistema.`,
      `Sí, ${accion}`,
      'Cancelar'
    );

    if (!confirmado) {
      return;
    }

    this.alertService.mostrarLoader(`${item.activo ? 'Desactivando' : 'Activando'} usuario...`);
    
    this.service.toggleActivo(item.idParticipante).subscribe({
      next: () => {
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaConTimer(
          '¡Estado actualizado!', 
          `El usuario ha sido ${item.activo ? 'desactivado' : 'activado'}.`, 
          1800, 
          'success'
        );
        this.cargarDatos(); 
      },
      error: (err) => {
        console.error(err);
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaError('Error al cambiar el estado del usuario.');
      }
    });
  }

  async prepararEliminar(item: ParticipanteDTO): Promise<void> {
    const confirmado = await this.alertService.confirmarAccion(
      '¿Eliminar participante?',
      `Se eliminará el participante: "${item.nombres} ${item.apellidos}" y su usuario asociado.`,
      'Sí, eliminar',
      'Cancelar'
    );

    if (confirmado) {
      this.confirmarEliminacion(item.idParticipante);
    }
  }

  private confirmarEliminacion(id: number): void {
    this.alertService.mostrarLoader('Eliminando participante...');
    this.service.eliminar(id).subscribe({
      next: () => {
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaConTimer('¡Eliminado!', 'El participante ha sido eliminado.', 2000, 'success');
        this.cargarDatos();
      },
      error: (err) => {
        console.error(err);
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaError('Ocurrió un error al eliminar el participante.');
      }
    });
  }
}