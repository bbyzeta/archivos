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
import { ArgumentoEvaluacion, CriterioEvaluacion } from '../../models/jurado.model';
import { JuradoService } from '../../services/jurado.service';
import { Evaluacion } from '../../models/evaluacion.model';
import { ModalEvaluacionComponent } from '../modal-evaluacion-component/modal-evaluacion-component';

@Component({
  selector: 'app-jurado-expediente-component',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalEvaluacionComponent],
  templateUrl: './jurado-expediente-component.html',
  styleUrl: './jurado-expediente-component.scss',
})
export class JuradoExpedienteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly alertaService = inject(AlertaService);
  private readonly auditorService = inject(AuditorService);
  private readonly juradoService = inject(JuradoService);
  private readonly sanitizer = inject(DomSanitizer);

  private readonly apiArchivosUrl = environment.apiUrl + '/archivos/view';

  cargando = signal<boolean>(true);
  procesandoAccion = signal<boolean>(false);
  expediente = signal<ExpedienteAuditoriaDTO | null>(null);

  documentoSeleccionado = signal<ItemRevisionDocumento | null>(null);
  urlVisorPdf = signal<SafeResourceUrl | null>(null);

  listaCriterios = signal<CriterioEvaluacion[] | null>([]);

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

  habilitaBtnEvaluacion:any;

  abrirModal = false;

  permiteProponer = signal<boolean>(false);

  constructor() {
    effect(() => {
      const url = this.urlVisorPdf();
      if (url && !url.toString().toLowerCase().includes('/pdf/')) {
        this.descargarArchivo();
      }
    });
  }

  async ngOnInit(): Promise<void> {
    const rawRole = sessionStorage.getItem('rol')?.replaceAll('"', '') || 'Auditor';
    this.currentUserRole.set(rawRole);

    const idExpediente = this.route.snapshot.paramMap.get('idExpediente');
    if (idExpediente) {
      await this.cargarExpediente(Number(idExpediente));
      await this.obtenerCriterios();
      await this.validaPermiteProponer();
    } else {
      this.alertaService.mostrarAlertaError('No se especificó un expediente a auditar.');
      this.volver();
    }
  }

  async validaPermiteProponer(): Promise<void>{
    try {
      let sidJuez = sessionStorage.getItem('idUsuario');
      let idJuez = Number(sidJuez ?? 0);
      const argumento : ArgumentoEvaluacion={
        idArgumento: 0,
        idConvocatoria: this.expediente()!.idConvocatoria,
        idCriterio: 0,
        idUsuario: this.expediente()!.idPostulante,
        idJuez: idJuez,
        argumento: ''
      }
      const permite = await firstValueFrom(this.juradoService.validaPermiteProponer(argumento));
      this.permiteProponer.set(permite) ;
    } catch (error) {
      
    }
  }

  async obtenerCriterios(): Promise<void>{
    try {
      const dataCriterios = await firstValueFrom(this.juradoService.obtenerCriterios(this.expediente()!.idConvocatoria, this.expediente()!.idPostulante!));
      this.listaCriterios.set(dataCriterios);
    } catch (err) {
      this.alertaService.mostrarAlerta(
        'Error',
        'No se pudo recuperar la información de los criterios.',
        'error',
        'Aceptar',
        '#9F2241'
      );
    } finally {
      this.cargando.set(false);
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
      this.alertaService.mostrarAlertaError('No se pudo guardar la evaluación del documento.');
    }
  }

  async emitirAprobacionAuditor(): Promise<void> {
    const exp = this.expediente();
    if (!exp) return;
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
    this.router.navigate(['/jurado/postulantes']);
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

  actualizarListaCriterios(id: number, nuevoValor: number) {
    this.listaCriterios.update(items =>
      items!.map(item =>
        item.idCriterio === id ? { ...item, porcentajeAsignado: nuevoValor } : item
      )
    );
  }

  async guardarEvaluacion(){
    const tieneCero = this.listaCriterios()?.some(
      item => item.porcentajeAsignado == 0 || item.porcentajeAsignado == null
    ) ?? false;

    let confirm = false;

    if(tieneCero){
      confirm = await this.alertaService.confirmarAccion(
        'Atención',
        'La evaluación de criterios contiene valores en 0, ¿Está seguro de continuar?',           
      );

      if(!confirm) return;
    }


    if(!tieneCero){
      const confirmn = await this.alertaService.confirmarAccion(
        'Atención',
        'La evaluación de criterios se guardará con los valores asignados, ¿Está seguro de continuar?',           
      );

      if(!confirmn) return;
    }

    this.listaCriterios.update(criterios =>
      criterios!.map(item => ({
        ...item,
        porcentajeAsignado: item.porcentajeAsignado ?? 0
      }))
    );

    let dto:Evaluacion[]=[]; 

    this.listaCriterios()?.forEach(item =>{
      let objEva:Evaluacion={
        idEvaluacion:0,
        idExpediente:0,
        idCriterio:item.idCriterio,
        idUsuario:this.expediente()!.idPostulante!,
        porcentajeAsignado:item.porcentajeAsignado,
        criterioEvaluado:1,
        fechaEvaluacion:""
      }
      dto.push(objEva);
    });

    this.alertaService.mostrarLoader('Guardando Evaluación...');
    this.juradoService.guardarEvaluacionPostulante(dto).subscribe({
      next: (data) => {
        setTimeout(() => {
        this.alertaService.cerrarLoader();
        window.location.reload();  
        }, 1000);
        
      },
      error: (err)=>{
        this.alertaService.cerrarLoader();
        this.alertaService.mostrarAlertaError('No se pudo guardar la información de evaluación.');
      }
    });

  }

  validaGuardarEvaluacion(){
    this.habilitaBtnEvaluacion = this.listaCriterios()?.some(item =>  item.criterioEvaluado == null);
    
    return this.habilitaBtnEvaluacion ? false : true;

  }

  validaProponerPostulante(){
    if(this.habilitaBtnEvaluacion){
      return true;
    }else{
      return false;
    }    
  }

  mostrarModalProponerPostulante(){
    this.abrirModal = true;
  }

  guardarEvaluacionArg(val: CriterioEvaluacion[]){

    let listaArgumentos : ArgumentoEvaluacion[] = []
    let sidJuez = sessionStorage.getItem('idUsuario');
    let idJuez = Number(sidJuez ?? 0);

    val.forEach(item => {
      let obj:ArgumentoEvaluacion = {
        idConvocatoria: this.expediente()!.idConvocatoria,
        idCriterio: item.idCriterio,
        idUsuario: this.expediente()!.idPostulante,
        idJuez: idJuez,
        argumento: item.argumentos ?? "",
        idArgumento: 0
      }
      listaArgumentos.push(obj);
    });

    this.alertaService.mostrarLoader('Guardando Argumentos...');
    this.juradoService.guardarArgumentosPostulante(listaArgumentos).subscribe({
      next: (data) =>{
        setTimeout(() => {
          this.alertaService.cerrarLoader();          
          this.abrirModal = false;
          window.location.reload();
        }, 1000);
      },
      error: (err) =>{
        this.alertaService.cerrarLoader();
        this.alertaService.mostrarAlertaError('No se pudo guardar la información de argumentos.');
      },
    });

  }

  
}
