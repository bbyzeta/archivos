import { Component, OnInit, signal, output, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ConvocatoriaService } from '../../services/convocatoria.service';
import { ConvocatoriaResponseDTO } from '../../models/convocatorias.model';
import { environment } from '../../../environments/environment.development';
import { AlertaService } from '../alerts/alert-component';
import { JuradoService } from '../../services/jurado.service';
import { JuradoItem } from '../../models/jurado.models';

declare const bootstrap: any;

@Component({
  selector: 'app-consulta-convocatorias',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './consulta-convocatorias-component.html',
  styleUrls: ['./consulta-convocatorias-component.scss']
})
export class ConsultaConvocatoriasComponent implements OnInit {
  private readonly service = inject(ConvocatoriaService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly alertService = inject(AlertaService);
  private readonly juradoService = inject(JuradoService);

  readonly apiArchivosUrl = environment.apiUrl + '/archivos/view';

  onCrearNueva = output<void>();
  onEditarConvocatoria = output<ConvocatoriaResponseDTO>();

  convocatorias = signal<ConvocatoriaResponseDTO[]>([]);
  cargando = signal<boolean>(false);
  terminoBusqueda = signal<string>('');

  paginaActual = signal<number>(0);
  tamanioPagina = signal<number>(5);
  totalElementos = signal<number>(0);
  totalPaginas = signal<number>(0);

  convocatoriaSeleccionada = signal<ConvocatoriaResponseDTO | null>(null);


  convocatoriaSeleccionadaParaJurados = signal<any | null>(null);
  juradosDisponibles = signal<JuradoItem[]>([]);
  juradosSeleccionadosIds = signal<Set<number>>(new Set<number>());
  filtroJuradosModal = signal<string>('');

  juradosFiltrados = computed(() => {
    const query = this.filtroJuradosModal().toLowerCase().trim();
    if (!query) return this.juradosDisponibles();

    return this.juradosDisponibles().filter(j => 
      `${j.nombres} ${j.apellidos}`.toLowerCase().includes(query) ||
      j.institucionAdscripcion.toLowerCase().includes(query) ||
      j.areaConocimiento.toLowerCase().includes(query)
    );
  });

  juradosDetalleConvocatoria = signal<JuradoItem[]>([]);
  cargandoJuradosDetalle = signal<boolean>(false);

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando.set(true);
    this.service.listar(this.paginaActual(), this.tamanioPagina(), this.terminoBusqueda())
      .subscribe({
        next: (resp) => {
          this.convocatorias.set(resp.content || []);
          this.totalElementos.set(resp.totalElements || 0);
          this.totalPaginas.set(resp.totalPages || 0);
          this.cargando.set(false);
        },
        error: (err) => {
          console.error('Error al listar convocatorias', err);
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

  obtenerEstado(apertura: string, cierre: string, activa: boolean) {
    if (!activa) return { texto: 'INACTIVA', badgeClass: 'badge-inactiva' };
    const now = new Date();
    const fApertura = new Date(apertura);
    const fCierre = new Date(cierre);

    if (now < fApertura) return { texto: 'PROGRAMADA', badgeClass: 'badge-programada' };
    if (now >= fApertura && now <= fCierre) return { texto: 'VIGENTE', badgeClass: 'badge-vigente' };
    return { texto: 'CERRADA', badgeClass: 'badge-cerrada' };
  }

  obtenerUrlSeguraPdf(nombreArchivoPdf?: string): SafeResourceUrl | null {
    if (!nombreArchivoPdf) return null;
    const nombreLimpio = nombreArchivoPdf.replace(/^.*[\\\/]/, '');
    const urlCompleta = `${this.apiArchivosUrl}/pdf/${nombreLimpio}`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(urlCompleta);
  }

  obtenerUrlImagen(nombreArchivoImg?: string): string {
    if (!nombreArchivoImg) return '';
    const nombreLimpio = nombreArchivoImg.replace(/^.*[\\\/]/, '');
    return `${this.apiArchivosUrl}/img/${nombreLimpio}`;
  }

  obtenerUrlDescargaPdf(nombreArchivoPdf?: string): string {
    if (!nombreArchivoPdf) return '';
    const nombreLimpio = nombreArchivoPdf.replace(/^.*[\\\/]/, '');
    return `${this.apiArchivosUrl}/pdf/${nombreLimpio}`;
  }

  async toggleActiva(conv: ConvocatoriaResponseDTO): Promise<void> {
    const nuevoEstadoTexto = conv.activa ? 'desactivar' : 'activar';
    const titulo = `¿Deseas ${nuevoEstadoTexto} la convocatoria?`;
    const mensaje = `La convocatoria "${conv.titulo}" cambiará su estado a ${conv.activa ? 'Inactiva' : 'Activa'}.`;
    const textoBoton = conv.activa ? 'Sí, desactivar' : 'Sí, activar';

    const confirmado = await this.alertService.confirmarAccion(
      titulo,
      mensaje,
      textoBoton,
      'Cancelar'
    );

    if (!confirmado) {
      this.cargarDatos();
      return;
    }

    this.alertService.mostrarLoader(`${conv.activa ? 'Desactivando' : 'Activando'} convocatoria...`);

    this.service.toggleActiva(conv.id).subscribe({
      next: () => {
        this.alertService.cerrarLoader();
        
        this.alertService.mostrarAlertaConTimer(
          '¡Estado actualizado!',
          `La convocatoria se ha ${conv.activa ? 'desactivado' : 'activado'} correctamente.`,
          1800,
          'success'
        );

        this.cargarDatos();
      },
      error: (err) => {
        console.error('Error al cambiar estado de la convocatoria', err);
        this.alertService.cerrarLoader();
        
        this.alertService.mostrarAlertaError(
          'No se pudo cambiar el estado de la convocatoria. Inténtalo de nuevo.'
        );

        this.cargarDatos();
      }
    });
  }

  abrirDetalle(item: any): void {
    this.convocatoriaSeleccionada.set(item);
    this.juradosDetalleConvocatoria.set([]);
    this.cargandoJuradosDetalle.set(true);

    const modalEl = document.getElementById('modalDetalleConfig');
    const modal = modalEl ? (bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl)) : null;
    modal?.show();

    this.juradoService.obtenerJuradosCompletosPorConvocatoria(item.id).subscribe({
      next: (jurados: JuradoItem[]) => {
        this.juradosDetalleConvocatoria.set(jurados || []);
        this.cargandoJuradosDetalle.set(false);
      },
      error: (err: any) => {
        console.error('Error al cargar jurados del detalle:', err);
        this.cargandoJuradosDetalle.set(false);
      }
    });
  }

  abrirAsignacionJuradosDesdeDetalle(convocatoria: any): void {
    const modalDetalleEl = document.getElementById('modalDetalleConfig');
    const modalDetalle = modalDetalleEl ? bootstrap.Modal.getInstance(modalDetalleEl) : null;
    modalDetalle?.hide();

    setTimeout(() => {
      this.abrirAsignacionJurados(convocatoria);
    }, 350);
  }

  editar(conv: ConvocatoriaResponseDTO): void {
    this.onEditarConvocatoria.emit(conv);
  }

  async prepararEliminar(conv: ConvocatoriaResponseDTO): Promise<void> {
    const confirmado = await this.alertService.confirmarAccion(
      '¿Eliminar convocatoria?',
      `Se eliminará la convocatoria: "${conv.titulo}".`,
      'Sí, eliminar',
      'Cancelar'
    );

    if (confirmado) {
      this.confirmarEliminacion(conv.id);
    }
  }

  private confirmarEliminacion(id: number): void {
    this.alertService.mostrarLoader('Eliminando convocatoria...');

    this.service.eliminar(id).subscribe({
      next: () => {
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaConTimer(
          '¡Eliminado!',
          'La convocatoria ha sido eliminada correctamente.',
          2000,
          'success'
        );
        this.cargarDatos();
      },
      error: (err) => {
        console.error('Error al eliminar', err);
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaError(
          'Ocurrió un error al intentar eliminar la convocatoria.'
        );
      }
    });
  }


  abrirAsignacionJurados(convocatoria: any): void {
    this.convocatoriaSeleccionadaParaJurados.set(convocatoria);
    this.filtroJuradosModal.set('');
    this.alertService.mostrarLoader('Cargando evaluadores disponibles...');

    this.juradoService.listarPaginado(0, 100, '').subscribe({
      next: (respJurados: any) => {
        const activos = (respJurados.content || []).filter((j: JuradoItem) => j.activo);
        this.juradosDisponibles.set(activos);

        this.juradoService.obtenerJuradosAsignados(convocatoria.id).subscribe({
          next: (idsAsignados: number[]) => {
            this.juradosSeleccionadosIds.set(new Set(idsAsignados));
            this.alertService.cerrarLoader();

            const modalEl = document.getElementById('modalAsignarJurados');
            if (modalEl) {
              const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
              modal.show();
            }
          },
          error: (err: any) => {
            console.error(err);
            this.alertService.cerrarLoader();
            this.alertService.mostrarAlertaError('No se pudieron obtener los jurados asignados.');
          }
        });
      },
      error: (err: any) => {
        console.error(err);
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaError('No se pudo cargar la lista de jurados.');
      }
    });
  }

  estaJuradoSeleccionado(idJurado: number): boolean {
    return this.juradosSeleccionadosIds().has(idJurado);
  }

  toggleSeleccionJurado(idJurado: number): void {
    const actual = new Set(this.juradosSeleccionadosIds());
    if (actual.has(idJurado)) {
      actual.delete(idJurado);
    } else {
      actual.add(idJurado);
    }
    this.juradosSeleccionadosIds.set(actual);
  }

  guardarAsignacionJurados(): void {
    const convocatoria = this.convocatoriaSeleccionadaParaJurados();
    if (!convocatoria) return;

    const idsParaGuardar = Array.from(this.juradosSeleccionadosIds());

    this.alertService.mostrarLoader('Guardando asignación de jurados...');
    this.juradoService.asignarJuradosAConvocatoria(convocatoria.id, idsParaGuardar).subscribe({
      next: () => {
        this.alertService.cerrarLoader();
        const modalEl = document.getElementById('modalAsignarJurados');
        const modal = modalEl ? bootstrap.Modal.getInstance(modalEl) : null;
        modal?.hide();

        this.alertService.mostrarAlertaConTimer(
          '¡Asignación Guardada!',
          `Se asignaron ${idsParaGuardar.length} evaluador(es) a la convocatoria.`,
          2000,
          'success'
        );
      },
      error: (err: any) => {
        console.error(err);
        this.alertService.cerrarLoader();
        this.alertService.mostrarAlertaError('Error al guardar la asignación del comité.');
      }
    });
  }
}