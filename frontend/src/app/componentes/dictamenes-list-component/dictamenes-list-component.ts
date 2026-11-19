import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DictamenService } from '../../services/dictamen.service';
import { DictamenReporte } from '../../models/dictamen.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dictamenes-list-component',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dictamenes-list-component.html',
  styleUrl: './dictamenes-list-component.scss',
})
export class DictamenesListComponent implements OnInit {
  private dictamenService = inject(DictamenService);
  private readonly authService = inject(AuthService);

  protected readonly Math = Math;

  dictamenes = signal<DictamenReporte[]>([]);
  cargando = signal<boolean>(false);
  
  terminoBusqueda = signal<string>('');
  convocatoriaFiltroId = signal<number | null>(null);

  dictamenSeleccionado = signal<DictamenReporte | null>(null);

  totalElementos = computed(() => this.dictamenes().length);


  paginaActual = signal<number>(1);
  itemsPorPagina = signal<number>(20); 
  maxPaginasVisibles = 10; 

  totalItems = computed(() => this.dictamenes().length);
  totalPaginas = computed(() => Math.ceil(this.totalItems() / this.itemsPorPagina()) || 1);

  dictamenesPaginados = computed(() => {
    const inicio = (this.paginaActual() - 1) * this.itemsPorPagina();
    const fin = inicio + this.itemsPorPagina();
    return this.dictamenes().slice(inicio, fin);
  });

  paginasVisibles = computed(() => {
    const total = this.totalPaginas();
    const actual = this.paginaActual();
    const max = this.maxPaginasVisibles;

    let inicio = Math.floor((actual - 1) / max) * max + 1;
    let fin = Math.min(inicio + max - 1, total);

    const paginas: number[] = [];
    for (let i = inicio; i <= fin; i++) {
      paginas.push(i);
    }
    return paginas;
  });

  cambiarPagina(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginas()) {
      this.paginaActual.set(pagina);
    }
  }

  ngOnInit(): void {
    this.buscar();
  }

  buscar(): void {
    this.paginaActual.set(1);
    this.cargando.set(true);
    this.dictamenService.obtenerDictamenes(this.convocatoriaFiltroId(), null, this.terminoBusqueda()).subscribe({
      next: (data) => {
        this.dictamenes.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('Error al cargar dictámenes:', err);
        this.cargando.set(false);
      }
    });
  }

  limpiarFiltros(): void {
    this.paginaActual.set(1);
    this.terminoBusqueda.set('');
    this.convocatoriaFiltroId.set(null);
    this.buscar();
  }

  abrirDetalleCriterios(item: DictamenReporte): void {
    this.dictamenSeleccionado.set(item);
  }

  cerrarDetalle(): void {
    this.dictamenSeleccionado.set(null);
  }
}
