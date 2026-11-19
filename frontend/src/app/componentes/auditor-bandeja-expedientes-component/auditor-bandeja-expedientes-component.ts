import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { AlertaService } from '../alerts/alert-component';
import { AuditorService } from '../../services/auditor.service';
import { ConvocatoriaService } from '../../services/convocatoria.service';
import { ConvocatoriaResponseDTO } from '../../models/convocatorias.model';
import { ExpedienteBandejaItem } from '../../models/auditor.model';


@Component({
  selector: 'app-auditor-bandeja-expedientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auditor-bandeja-expedientes-component.html',
  styleUrls: ['./auditor-bandeja-expedientes-component.scss']
})
export class AuditorBandejaExpedientesComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly alertaService = inject(AlertaService);
  private readonly auditorService = inject(AuditorService);
  private readonly convocatoriaService = inject(ConvocatoriaService);

  cargando = signal<boolean>(true);
  terminoBusqueda = signal<string>('');
  filtroEstado = signal<string>('TODOS');
  convocatoriaFiltroId = signal<number | null>(null);

  convocatorias = signal<ConvocatoriaResponseDTO[]>([]);
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

  ngOnInit(): void {
    const rawRole = sessionStorage.getItem('rol')?.replaceAll('"', '') || 'Auditor';
    this.currentUserRole.set(rawRole);

    this.cargarConvocatorias();
    this.cargarBandeja();
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
      const data = await firstValueFrom(
        this.auditorService.listarExpedientesParaAuditoria(
          this.paginaActual(),
          this.tamanioPagina(),
          this.terminoBusqueda(),
          this.convocatoriaFiltroId(),
          this.filtroEstado() === 'TODOS' ? '' : this.filtroEstado()
        )
      );

      this.expedientes.set(data.content || []);
      this.totalPaginas.set(data.totalPages || 1);
      this.totalElementos.set(data.totalElements || 0);
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
    this.router.navigate(['/auditor/expediente', item.idExpediente]);
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
}