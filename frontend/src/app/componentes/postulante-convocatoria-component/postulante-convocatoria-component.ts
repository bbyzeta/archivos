import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AuthService } from '../../services/auth.service';
import { ConvocatoriaService } from '../../services/convocatoria.service';
import { AlertaService } from '../alerts/alert-component';
import { ConvocatoriaResponseDTO } from '../../models/convocatorias.model';
import { environment } from '../../../environments/environment.development';
import { firstValueFrom } from 'rxjs';
import { PostulacionService } from '../../services/postulacion.service';
import { EstadoExpediente, PostulacionResponseDTO } from '../../models/expediente.model';



@Component({
  selector: 'app-postulante-convocatoria-component',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './postulante-convocatoria-component.html',
  styleUrl: './postulante-convocatoria-component.scss',
})
export class PostulanteConvocatoriaComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly convocatoriaService = inject(ConvocatoriaService);
  private readonly alertService = inject(AlertaService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly router = inject(Router);
  private readonly apiArchivosUrl = environment.apiUrl + '/archivos/view';
  private readonly postulacionService = inject(PostulacionService);
  private readonly route = inject(ActivatedRoute);

  cargando = signal<boolean>(true);
  
  convocatoria = signal<ConvocatoriaResponseDTO | null>(null);

  currentUserEmail = signal<string>(this.authService.getUserName() || 'postulante@csg.gob.mx');
  currentUserRole = signal<string>(this.authService.getRol() || 'Postulante');

  expediente = signal<PostulacionResponseDTO | null>(null);
  
  estadoExpediente = signal<EstadoExpediente | null >('BORRADOR');

  idParam:string = "";
  conObservaciones = false;

  nombreDocConv = signal<string>("");
  nombreDocGuia = signal<string>("");

  ngOnInit(): void {
    this.idParam = this.route.snapshot.paramMap.get('id') || "0";
    this.cargarConvocatoriaVigente(this.idParam);
    this.cargarConvocatoriaEstado();
    
  }

  private cargaDocs(){
    if(this.convocatoria()!.idGrupoPremio===1){
      this.nombreDocConv.set("1.-Convocatoria PSP.pdf") ;
      this.nombreDocGuia.set("1.-Guia de Postulacion.pdf");
    }
  }

  private obtenerIdUsuarioSesion(): number {
    const rawId = sessionStorage.getItem('idUsuario') || localStorage.getItem('idUsuario');
    return rawId ? Number(rawId.replaceAll('"', '')) : 1;
  }

  cargarConvocatoriaVigente(idParam:string): void {
    this.cargando.set(true);
    this.convocatoriaService.obtenerConvocatoriaPorId(idParam).subscribe({
      next: (data: ConvocatoriaResponseDTO) => {
        this.convocatoria.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('Error al cargar la convocatoria:', err);
        this.cargando.set(false);
        this.alertService.mostrarAlerta(
          'Aviso',
          'En este momento no hay una convocatoria abierta para postulación.',
          'info',
          'Aceptar',
          '#10312B'
        );
      }
    });
  }

    async cargarConvocatoriaEstado(): Promise<void> {
      try {
        const convData = await firstValueFrom(this.convocatoriaService.obtenerConvocatoriaPorId(this.idParam));
        this.convocatoria.set(convData);
  
        const expData = await firstValueFrom(
          this.postulacionService.iniciarOObtenerBorrador({
            idConvocatoria: convData.id,
            idUsuario: this.obtenerIdUsuarioSesion(),
            tituloProyecto: convData.titulo,
            resumenEjecutivo: ''
          })
        );

        console.log("****" + JSON.stringify(expData));
        const tieneObservaciones: boolean = expData.documentos!.some(item => item.estadoRevision === 'CON_OBSERVACIONES');
        const tieneObservacionesAnex: boolean = expData.anexos!.some(item => item.estadoRevision === 'CON_OBSERVACIONES');
        this.conObservaciones = tieneObservaciones || tieneObservacionesAnex;

  
        this.expediente.set(expData);
        this.estadoExpediente.set(expData.estado as EstadoExpediente);

        this.cargaDocs();
        
      } catch (err) {
        console.error(err);
        this.alertService.mostrarAlerta(
          'Error',
          'No se encontró una convocatoria activa disponible.',
          'error',
          'Aceptar',
          '#9b2247'
        );
      } finally {
      }
    }

  obtenerEstado(apertura: string, cierre: string, activa: boolean): { texto: string; badgeClass: string; permitePostular: boolean } {
    if (!activa) {
      return { texto: 'Convocatoria Inactiva', badgeClass: 'bg-secondary text-white', permitePostular: false };
    }
    const ahora = new Date().getTime();
    const fApertura = new Date(apertura).getTime();
    const fCierre = new Date(cierre).getTime();

    if (ahora < fApertura) {
      return { texto: 'Próxima a Iniciar', badgeClass: 'badge-proxima', permitePostular: false };
    } else if (ahora > fCierre) {
      return { texto: 'Convocatoria Cerrada', badgeClass: 'badge-cerrada', permitePostular: false };
    }
    return { texto: 'Convocatoria Vigente', badgeClass: 'badge-vigente', permitePostular: true };
  }

  obtenerUrlImagen(ruta?: string): string {
     if (!ruta) return '';
    const nombreLimpio = ruta.replace(/^.*[\\\/]/, '');
    return `${this.apiArchivosUrl}/img/${nombreLimpio}`;
  }

  obtenerUrlSeguraPdf(ruta?: string): SafeResourceUrl | null {
    if (!ruta) return null;
    const nombreLimpio = ruta.replace(/^.*[\\\/]/, '');
    const urlCompleta = `${this.apiArchivosUrl}/conv/${nombreLimpio}`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(urlCompleta);
  }

  obtenerUrlDescargaPdf(ruta?: string): string {
    if (!ruta) return '';
    const nombreLimpio = ruta.replace(/^.*[\\\/]/, '');
    return `${this.apiArchivosUrl}/conv/${nombreLimpio}`;
  }

  iniciarPostulacion(idConvocatoria: number): void {
    this.router.navigate(['/postulante/expediente', idConvocatoria]);
  }

  async logout(): Promise<void> {
    const confirmado = await this.alertService.confirmarAccion(
      '¿Cerrar Sesión?',
      'Se cerrará tu sesión del portal institucional de convocatorias.',
      'Sí, salir',
      'Cancelar'
    );

    if (confirmado) {
      this.authService.logout();
      this.router.navigate(['/login']);
    }
  }

  volver(): void {
    this.router.navigate(['/postulante/convocatorias']);
  }
}