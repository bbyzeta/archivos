import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { AlertaService } from '../alerts/alert-component';
import { AuthService } from '../../services/auth.service';
import { ConvocatoriaGeneralService } from '../../services/convocatoria-general.service';
import { ConvocatoriaConImagen, ConvocatoriaGeneralResponseDTO } from '../../models/convocatorias-general.model';


@Component({
  selector: 'app-postulante-convocatorias-component',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './postulante-convocatorias-component.html',
  styleUrl: './postulante-convocatorias-component.scss',
})
export class PostulanteConvocatoriasComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly alertService = inject(AlertaService);
  private readonly convocatoriaService = inject(ConvocatoriaGeneralService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly router = inject(Router);

  currentUserEmail = signal<string>(this.authService.getUserName() || 'postulante@csg.gob.mx');
  currentUserRole = signal<string>(this.authService.getRol() || 'Postulante');

  categorias = signal<ConvocatoriaGeneralResponseDTO[]>([]);
  categoriaSeleccionadaId = signal<number | null>(null);
  
  convocatoriasHijas = signal<ConvocatoriaConImagen[]>([]);
  cargandoHijas = signal<boolean>(false);

  private objectUrls: string[] = [];

  ngOnInit(): void {
    this.cargarCategoriasPadre();
  }

  ngOnDestroy(): void {
    this.limpiarObjectUrls();
  }

  cargarCategoriasPadre(): void {
    this.convocatoriaService.listarConvocatorias().subscribe({
      next: (data) => {
        this.categorias.set(data);
        if (data.length > 0) {
          this.seleccionarCategoria(data[0].id);
        }
      },
      error: () => {
        this.alertService.mostrarAlerta('Aviso', 'Error al consultar categorías.', 'info', 'Aceptar', '#10312B');
      }
    });
  }

  seleccionarCategoria(idCategoria: number): void {
    this.categoriaSeleccionadaId.set(idCategoria);
    this.cargandoHijas.set(true);
    this.limpiarObjectUrls();

    this.convocatoriaService.obtenerHijasPorCategoria(idCategoria).subscribe({
      next: (hijas) => {
        const listaFinal: ConvocatoriaConImagen[] = hijas.map(h => ({ ...h }));
        this.convocatoriasHijas.set(listaFinal);
        this.cargandoHijas.set(false);

        listaFinal.forEach((item, index) => {
          if (item.rutaImagenHeader) {
            this.cargarImagenParaConvocatoria(item.rutaImagenHeader, index);
          }
        });
      },
      error: () => {
        this.convocatoriasHijas.set([]);
        this.cargandoHijas.set(false);
      }
    });
  }

  private cargarImagenParaConvocatoria(nombreImagen: string, index: number): void {
    this.convocatoriaService.obtenerImagenHeader(nombreImagen).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        this.objectUrls.push(objectUrl);
        
        const actual = [...this.convocatoriasHijas()];
        if (actual[index]) {
          actual[index].imagenUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
          this.convocatoriasHijas.set(actual);
        }
      },
      error: (err) => console.error('Error al cargar imagen:', err)
    });
  }

  private limpiarObjectUrls(): void {
    this.objectUrls.forEach(url => URL.revokeObjectURL(url));
    this.objectUrls = [];
  }

  async logout(): Promise<void> {
    const confirmado = await this.alertService.confirmarAccion(
      '¿Cerrar Sesión?',
      'Se cerrará tu sesión del portal.',
      'Sí, salir',
      'Cancelar'
    );
    if (confirmado) {
      this.authService.logout();
      this.router.navigate(['/login']);
    }
  }

  verDetalle(idConvocatoria:number){
    this.router.navigate(['postulante/convocatoria-vigente/'+idConvocatoria]);
  }
}