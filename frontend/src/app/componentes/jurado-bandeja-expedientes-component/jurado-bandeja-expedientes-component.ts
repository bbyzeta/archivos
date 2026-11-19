import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { AlertaService } from '../alerts/alert-component';
import { AuditorService } from '../../services/auditor.service';
import { ConvocatoriaService } from '../../services/convocatoria.service';
import { ConvocatoriaJuradoDTO, ConvocatoriaResponseDTO } from '../../models/convocatorias.model';
import { ExpedienteBandejaItem } from '../../models/auditor.model';
import { JuradoExpedienteService } from '../../services/jurado.expediente.service';

@Component({
  selector: 'app-jurado-bandeja-expedientes-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './jurado-bandeja-expedientes-component.html',
  styleUrl: './jurado-bandeja-expedientes-component.scss',
})
export class JuradoBandejaExpedientesComponent  implements OnInit {
  
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly alertaService = inject(AlertaService);
  private readonly juradoExpedienteService = inject(JuradoExpedienteService);
  private readonly convocatoriaService = inject(ConvocatoriaService);

  cargando = signal<boolean>(true);
  terminoBusqueda = signal<string>('');
  filtroEstado = signal<string>('TODOS');
  convocatoriaFiltroId = signal<number | null>(null);

  convocatorias = signal<ConvocatoriaResponseDTO[]>([]);
  convocatoriaJurado = signal<ConvocatoriaJuradoDTO[]>([]);
  expedientes = signal<ExpedienteBandejaItem[]>([]);

  paginaActual = signal<number>(0);
  tamanioPagina = signal<number>(10);
  totalPaginas = signal<number>(1);
  totalElementos = signal<number>(0);

  currentUserEmail = signal<string>(this.authService.getUserName() || 'auditor@csg.gob.mx');
  currentUserRole = signal<string>('Auditor');

  expedientesFiltrados = computed(() => {
    let items = this.expedientes();
    const query = this.terminoBusqueda().toLowerCase().trim();
    const estado = this.filtroEstado();

    if (query) {
      items = items.filter(e =>
        e.folio.toLowerCase().includes(query) ||
        e.nombrePostulante.toLowerCase().includes(query) ||
        e.curpPostulante.toLowerCase().includes(query) ||
        (e.institucionPostulante && e.institucionPostulante.toLowerCase().includes(query))
      );
    }

    if (estado !== 'TODOS') {
      items = items.filter(e => e.estadoExpediente === estado);
    }

    return items;
  });

  async ngOnInit(): Promise<void> {
    const rawRole = sessionStorage.getItem('rol')?.replaceAll('"', '') || 'Auditor';
    this.currentUserRole.set(rawRole);

    await this.obtenerIdConvocatoria();
    this.cargarConvocatorias();
    await this.cargarBandeja();
  }

  async obtenerIdConvocatoria(): Promise<void>{
    try {
      let idUsuario = this.authService.getIdUsuario() ? this.authService.getIdUsuario() : 0;
      const res = await firstValueFrom(this.juradoExpedienteService.getIdCategoria(idUsuario!));
      this.convocatoriaJurado.set(res);
      console.log(this.convocatoriaJurado());
    } catch (error) {
      console.error('Error al cargar convocatorias para filtro:', error);
    }
  }

  async cargarConvocatorias(): Promise<void> {
    try {
      const resp = await firstValueFrom(this.convocatoriaService.listar(0, 100, ''));
      this.convocatorias.set(resp.content || []);
    } catch (error) {
      console.error('Error al cargar convocatorias para filtro:', error);
    }
  }

  async cargarBandeja(): Promise<void> {
    this.cargando.set(true);
    try {
      console.log(JSON.stringify(this.convocatoriaJurado()));
      const ids = this.convocatoriaJurado().map(cat => cat.id);
      console.log(JSON.stringify(ids));
      const data = await firstValueFrom(
        this.juradoExpedienteService.listarExpedientesParaAuditoria(
          this.paginaActual(),
          this.tamanioPagina(),
          this.terminoBusqueda(),
          this.convocatoriaFiltroId(),
          this.filtroEstado() === 'TODOS' ? '' : this.filtroEstado(),
          ids
        )
      );

      this.expedientes.set(data.content || []);
      this.totalPaginas.set(data.totalPages || 1);
      this.totalElementos.set(data.totalElements || 0);
      this.cargarMetricasPostulantes();
    } catch (err) {
      console.error(err);
      this.alertaService.mostrarAlerta(
        'Error',
        'No se pudo recuperar la lista de expedientes de postulantes.',
        'error',
        'Aceptar',
        '#9F2241'
      );
    } finally {
      this.cargando.set(false);
    }
  }

  buscar(): void {
    this.paginaActual.set(0);
    this.cargarBandeja();
  }

  limpiarFiltros(): void {
    this.terminoBusqueda.set('');
    this.filtroEstado.set('TODOS');
    this.convocatoriaFiltroId.set(null);
    this.buscar();
  }

  cambiarPagina(pag: number): void {
    if (pag >= 0 && pag < this.totalPaginas() && pag !== this.paginaActual()) {
      this.paginaActual.set(pag);
      this.cargarBandeja();
    }
  }

  obtenerArregloPaginas(): number[] {
    return Array.from({ length: this.totalPaginas() }, (_, i) => i);
  }

  auditarExpediente(item: ExpedienteBandejaItem): void {
    this.router.navigate(['/jurado/expediente', item.idExpediente]);
  }

  async logout(): Promise<void> {
    const ok = await this.alertaService.confirmarAccion(
      'Cerrar Sesión',
      '¿Deseas salir del sistema de convocatorias?',
      'Sí, Salir'
    );
    if (ok) {
      this.authService.logout();
      this.router.navigate(['/login']);
    }
  }

  cargarMetricasPostulantes(): void {
    let idJuezActual = this.authService.getIdUsuario() ? this.authService.getIdUsuario() : 0;

    this.expedientes().forEach(item => {
      this.juradoExpedienteService
        .obtenerTotalEvaluacion(item.idPostulante, item.idConvocatoria)
        .subscribe({
          next: (total: number) => {
          this.expedientes.update(lista =>
            lista.map(exp =>
              exp.idPostulante === item.idPostulante && exp.idConvocatoria === item.idConvocatoria
                ? { ...exp, totalPorcentaje: total }
                : exp
            )
          );
        },
          error: (err) => console.error(`Error total evaluacion ID ${item.idPostulante}:`, err)
        });

      this.juradoExpedienteService
        .esPostulantePropuesto(item.idConvocatoria, item.idPostulante, idJuezActual!)
        .subscribe({
         next: (estado: number) => {
            this.expedientes.update(lista =>
              lista.map(exp =>
                exp.idPostulante === item.idPostulante && exp.idConvocatoria === item.idConvocatoria
                  ? { ...exp, propuesto: estado }
                  : exp
              )
            );
          },
          error: (err) => console.error(`Error estatus propuesto ID ${item.idPostulante}:`, err)
        });
    });
  }

}
