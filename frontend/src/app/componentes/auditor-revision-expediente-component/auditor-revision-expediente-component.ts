import { Component, OnInit, signal, computed, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { AlertaService } from '../alerts/alert-component';
import { environment } from '../../../environments/environment.development';
import { ExpedienteAuditoriaDTO, ItemRevisionDocumento } from '../../models/auditor.model';
import { AuditorService } from '../../services/auditor.service';


@Component({
  selector: 'app-auditor-revision-expediente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auditor-revision-expediente-component.html',
  styleUrl: './auditor-revision-expediente-component.scss',
})
export class AuditorRevisionExpedienteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly alertaService = inject(AlertaService);
  private readonly auditorService = inject(AuditorService);
  private readonly sanitizer = inject(DomSanitizer);

  private readonly apiArchivosUrl = environment.apiUrl + '/archivos/view';

  cargando = signal<boolean>(true);
  procesandoAccion = signal<boolean>(false);
  expediente = signal<ExpedienteAuditoriaDTO | null>(null);

  documentoSeleccionado = signal<ItemRevisionDocumento | null>(null);
  urlVisorPdf = signal<SafeResourceUrl | null>(null);

  documentosObligatorios = computed(() => 
    (this.expediente()?.documentos || []).filter(d => !d.esAnexo)
  );

  documentosAnexos = computed(() => 
    (this.expediente()?.documentos || []).filter(d => d.esAnexo)
  );

  todosLosDocumentosRevisados = computed(() => {
    const docs = this.expediente()?.documentos || [];
    return docs.length > 0 && docs.every(d => d.estadoRevision !== 'PENDIENTE');
  });

  currentUserEmail = signal<string>(this.authService.getUserName() || 'auditor@csg.gob.mx');
  currentUserRole = signal<string>('Auditor');

  conObservaciones = false;

  urlCrudaActual: string = '';

  constructor() {
    effect(() => {
      const url = this.urlVisorPdf();
      if (url && !url.toString().toLowerCase().includes('/pdf/')) {
        this.descargarArchivo();
      }
    });
  }

  ngOnInit(): void {
    const rawRole = sessionStorage.getItem('rol')?.replaceAll('"', '') || 'Auditor';
    this.currentUserRole.set(rawRole);

    const idExpediente = this.route.snapshot.paramMap.get('idExpediente');
    if (idExpediente) {
      this.cargarExpediente(Number(idExpediente));
    } else {
      this.alertaService.mostrarAlertaError('No se especificó un expediente a auditar.');
      this.volver();
    }
  }

  async cargarExpediente(id: number): Promise<void> {
    this.cargando.set(true);
    try {
      const data = await firstValueFrom(this.auditorService.obtenerExpedienteParaAuditoria(id));
      this.expediente.set(data);

      if (data.documentos && data.documentos.length > 0) {
        this.seleccionarDocumento(data.documentos[0]);

         const tieneObservaciones: boolean = data.documentos.some(item => item.estadoRevision === 'CON_OBSERVACIONES');
         this.conObservaciones = tieneObservaciones;
      }
    } catch (err) {
      console.error(err);
      this.alertaService.mostrarAlerta(
        'Error',
        'No se pudo recuperar la información del expediente del postulante.',
        'error',
        'Aceptar',
        '#9F2241'
      );
    } finally {
      this.cargando.set(false);
    }
  }

  seleccionarDocumento(doc: ItemRevisionDocumento): void {
    console.log(JSON.stringify(doc));
    this.documentoSeleccionado.set(doc);
    let rawUrl ="";
    if(doc.nombreOriginal.includes(".pdf")){
      rawUrl = `${this.apiArchivosUrl}/pdf/${doc.rutaArchivo}#toolbar=1&navpanes=0`;
    }else{
      rawUrl = `${this.apiArchivosUrl}/xlsx/${doc.rutaArchivo}`;
    }

    this.urlCrudaActual = rawUrl;

    this.urlVisorPdf.set(this.sanitizer.bypassSecurityTrustResourceUrl(rawUrl));
  }

  async marcarAprobado(doc: ItemRevisionDocumento): Promise<void> {
    doc.estadoRevision = 'APROBADO';
    doc.observacionAuditor = '';
    await this.sincronizarEstadoDocumento(doc);
  }
  
  async marcarRechazado(doc: ItemRevisionDocumento): Promise<void> {
    doc.estadoRevision = 'RECHAZADO';
    doc.observacionAuditor = '';
    await this.sincronizarEstadoDocumento(doc);
  }

  async abrirModalRechazoDoc(doc: ItemRevisionDocumento): Promise<void> {

    if(doc.corregido > 0){
      this.alertaService.mostrarAlerta('Atención','Solo puedes observar una vez el documento', 'warning','Aceptar','#10312B');
      return;
    }

    const motivo = await this.alertaService.mostrarPromptInput(
      '¡Atención!',
      `Ingresa el motivo para: "${doc.nombre}"`,
      doc.observacionAuditor || ''
    );

    if (motivo !== null) {
      doc.estadoRevision = 'CON_OBSERVACIONES';
      doc.observacionAuditor = motivo;
      await this.sincronizarEstadoDocumento(doc);
    }
  }

  private async sincronizarEstadoDocumento(doc: ItemRevisionDocumento): Promise<void> {
    const exp = this.expediente();
    if (!exp) return;

    try {
      await firstValueFrom(this.auditorService.actualizarDictamenDocumento(exp.idExpediente, doc.idDocumento, {
        estado: doc.estadoRevision,
        observacion: doc.observacionAuditor
      }));
      this.expediente.set({ ...exp });
    } catch (error) {
      console.error(error);
      this.alertaService.mostrarAlertaError('No se pudo guardar la evaluación del documento.');
    }
  }

  async emitirAprobacionAuditor(): Promise<void> {
    const exp = this.expediente();
    if (!exp) return;
console.log(JSON.stringify(exp.documentos));
    const hayRechazados = exp.documentos.some(d => d.estadoRevision != 'APROBADO');

    if (hayRechazados) {
      this.alertaService.mostrarAlerta(
        'Inconsistencia en Revisión',
        'Existen documentos rechazados o con observaciones, con este estado no se puede aprobar el expediente.',
        'warning',
        'Entendido',
        '#10312B'
      );
      return;
    }

    const confirmado = await this.alertaService.confirmarAccion(
      '¿Aprobar Validación Documental?',
      `El expediente ${exp.folio} de ${exp.nombrePostulante} superará la fase de revisión documental y quedará listo para la asignación y evaluación por el Jurado Dictaminador.`,
      'Sí, Validar para Jurado',
      'Cancelar'
    );

    if (!confirmado) return;

    this.alertaService.mostrarLoader('Formalizando validación documental...');
    this.procesandoAccion.set(true);

    try {
      await firstValueFrom(this.auditorService.dictaminarExpedienteAuditor(exp.idExpediente, {
        resultado: 'ACEPTADO',
        comentarios: 'Expediente documental acreditado sin observaciones.'
      }));

      this.alertaService.cerrarLoader();
      this.alertaService.mostrarAlertaConTimer(
        '¡Aprobación Registrada!',
        'El expediente fue acreditado exitosamente y continuará a la fase de evaluación por el Jurado.',
        2000,
        'success'
      );
      this.volver();
    } catch (error) {
      this.alertaService.cerrarLoader();
      console.error(error);
      this.alertaService.mostrarAlertaError('Ocurrió un error al registrar la validación del expediente.');
    } finally {
      this.procesandoAccion.set(false);
    }
  }

  async emitirRechazoAuditor(): Promise<void> {
    const exp = this.expediente();
    if (!exp) return;

    const confirmado = await this.alertaService.confirmarAccion(
      '¿Emitir Rechazo Documental?',
      `Se notificará el rechazo sobre el expediente ${exp.folio}. El postulante podrá seguir visualizando las observaciones documentales registradas.`,
      'Sí, Proceder con Rechazo',
      'Cancelar'
    );

    if (!confirmado) return;

    this.alertaService.mostrarLoader('Registrando dictamen de rechazo...');
    this.procesandoAccion.set(true);

    try {
      await firstValueFrom(this.auditorService.dictaminarExpedienteAuditor(exp.idExpediente, {
        resultado: 'RECHAZADO',
        comentarios: 'Incumplimiento o inconsistencias en los requisitos documentales solicitados.'
      }));

      this.alertaService.cerrarLoader();
      this.alertaService.mostrarAlerta(
        'Dictamen Registrado',
        'El expediente ha sido marcado con observaciones/rechazo documental.',
        'info',
        'Aceptar',
        '#9F2241'
      );
      this.volver();
    } catch (error) {
      this.alertaService.cerrarLoader();
      console.error(error);
      this.alertaService.mostrarAlertaError('No se pudo procesar el rechazo del expediente.');
    } finally {
      this.procesandoAccion.set(false);
    }
  }

  obtenerUrlImagenPremio(ruta?: string): string {
    if (!ruta) return 'assets/images/premio-default.png';
    const clean = ruta.replace(/^.*[\\\/]/, '');
    return `${this.apiArchivosUrl}/img/${clean}`;
  }

  volver(): void {
    this.router.navigate(['/auditor/postulantes']);
  }

  async logout(): Promise<void> {
    const ok = await this.alertaService.confirmarAccion('Cerrar Sesión', '¿Deseas salir del sistema?', 'Sí, Salir');
    if (ok) {
      this.authService.logout();
      this.router.navigate(['/login']);
    }
  }
  
  descargarArchivo() {
    if (!this.urlCrudaActual) return;

    const link = document.createElement('a');
    link.href = this.urlCrudaActual;
    link.download = ''; 
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    }
}
