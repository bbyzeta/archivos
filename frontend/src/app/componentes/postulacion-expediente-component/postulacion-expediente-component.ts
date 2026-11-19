import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { ConvocatoriaService } from '../../services/convocatoria.service';
import { PostulacionService } from '../../services/postulacion.service';
import { AlertaService } from '../alerts/alert-component';

import { ConvocatoriaResponseDTO } from '../../models/convocatorias.model';
import { AnexoAdjuntoDTO, AnexoItem, DocumentoRequisitoItem, EstadoExpediente, PostulacionResponseDTO } from '../../models/expediente.model';
import { environment } from '../../../environments/environment.development';

const MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; 

@Component({
  selector: 'app-postulacion-expediente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './postulacion-expediente-component.html',
  styleUrls: ['./postulacion-expediente-component.scss']
})
export class PostulacionExpedienteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly convocatoriaService = inject(ConvocatoriaService);
  private readonly postulacionService = inject(PostulacionService);
  private readonly alertService = inject(AlertaService);
  private readonly apiArchivosUrl = environment.apiUrl + '/archivos/view';

  cargando = signal<boolean>(true);
  guardando = signal<boolean>(false);
  convocatoria = signal<ConvocatoriaResponseDTO | null>(null);
  expediente = signal<PostulacionResponseDTO | null>(null);

  estadoExpediente = signal<EstadoExpediente>('BORRADOR');
  motivoRechazoTotal = signal<string>('');

  documentosRequisitos = signal<DocumentoRequisitoItem[]>([]);
  anexos = signal<AnexoItem[]>([]);

  dragOverIndex = signal<number | null>(null);
  dragOverAnexoId = signal<string | null>(null);

  puedeModificarTodo = computed(() => this.estadoExpediente() === 'BORRADOR');
  estaRechazadoTotal = computed(() => this.estadoExpediente() === 'RECHAZADO');
  estaSoloLectura = computed(() => this.estadoExpediente() === 'ENVIADO' || this.estadoExpediente() === 'ACEPTADO');
  tieneObservaciones = computed(() => this.estadoExpediente() === 'CON_OBSERVACIONES');

  currentUserEmail = signal<string>(this.authService.getUserName() || 'postulante@csg.gob.mx');
  currentUserRole = signal<string>('POSTULANTE');

  idParam = "";
  conObservaciones = false;

  hayPendientes = signal<boolean | null>(false);

  ngOnInit(): void {
    const rawRole = sessionStorage.getItem('rol')?.replaceAll('"', '') || 'POSTULANTE';
    this.currentUserRole.set(rawRole);

    this.idParam = this.route.snapshot.paramMap.get('id')!;

    if (this.idParam) {
      this.cargarDatos(Number(this.idParam));
    } else {
      this.cargarConvocatoriaVigente();
    }
  }

  private obtenerIdUsuarioSesion(): number {
    const rawId = sessionStorage.getItem('idUsuario') || localStorage.getItem('idUsuario');
    return rawId ? Number(rawId.replaceAll('"', '')) : 1;
  }

  async cargarDatos(idConvocatoria: number): Promise<void> {
    this.cargando.set(true);
    try {
      const convData = await firstValueFrom(this.convocatoriaService.obtenerPorId(idConvocatoria));
      const index = convData.requisitos.findIndex(re=> re.nombre === 'Documentos probatorios de todo lo descrito en el CV (pdf)');
      convData.requisitos.splice(index,1);
      this.convocatoria.set(convData);

      const expData = await firstValueFrom(
        this.postulacionService.iniciarOObtenerBorrador({
          idConvocatoria: convData.id,
          idUsuario: this.obtenerIdUsuarioSesion(),
          tituloProyecto: convData.titulo,
          resumenEjecutivo: ''
        })
      );

      this.expediente.set(expData);
      this.documentosPendientes();
      this.estadoExpediente.set(expData.estado as EstadoExpediente);

      this.inicializarExpediente(convData, expData);
    } catch (err) {
      console.error(err);
      this.alertService.mostrarAlerta(
        'Error',
        'No se pudo inicializar la postulación para esta convocatoria.',
        'error',
        'Aceptar',
        '#9b2247'
      );
    } finally {
      this.cargando.set(false);
    }
  }

  async cargarConvocatoriaVigente(): Promise<void> {
    this.cargando.set(true);
    try {
      const convData = await firstValueFrom(this.convocatoriaService.obtenerConvocatoriaActiva());
      this.convocatoria.set(convData);

      const expData = await firstValueFrom(
        this.postulacionService.iniciarOObtenerBorrador({
          idConvocatoria: convData.id,
          idUsuario: this.obtenerIdUsuarioSesion(),
          tituloProyecto: convData.titulo,
          resumenEjecutivo: ''
        })
      );

      this.expediente.set(expData);
      this.estadoExpediente.set(expData.estado as EstadoExpediente);
      this.inicializarExpediente(convData, expData);
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
      this.cargando.set(false);
    }
  }

  private inicializarExpediente(conv: ConvocatoriaResponseDTO, exp: PostulacionResponseDTO): void {
    if (exp.motivoRechazo) {
      this.motivoRechazoTotal.set(exp.motivoRechazo);
    }

    const documentosGuardados = exp.documentos || [];

    const obligatorios: DocumentoRequisitoItem[] = (conv.requisitos || [])
      .filter(r => !r.esAnexo)
      .map(r => {
        const idReqReal = (r as any).idRequisito ?? (r as any).id_requisito;
        const archivoGuardado = documentosGuardados.find(d => d.idRequisito === idReqReal);
        
        const urlDescarga = archivoGuardado?.rutaArchivo.includes(".pdf") ? `${this.apiArchivosUrl}/pdf/${archivoGuardado?.rutaArchivo}` : `${this.apiArchivosUrl}/xlsx/${archivoGuardado?.rutaArchivo}`;

        return {
          idRequisito: idReqReal,
          nombre: r.nombre,
          esAnexo: false,
          nombreArchivoExistente: archivoGuardado?.nombreOriginal,
          rutaArchivoExistente: archivoGuardado?.rutaArchivo,
          observacion: archivoGuardado?.observacionAuditor || '',
          observacionAuditor: archivoGuardado?.observacionAuditor || '',
          estadoRevision: archivoGuardado?.estadoRevision || 'NO CARGADO',
          rechazadoPorObservacion: archivoGuardado?.estadoRevision === 'RECHAZADO' ||
            archivoGuardado?.estadoRevision === 'OBSERVADO' ||
            archivoGuardado?.estadoRevision === 'CON_OBSERVACIONES',
          urlDescarga: urlDescarga
        };
      });


    this.documentosRequisitos.set(obligatorios);

    const tieneObservaciones: boolean = obligatorios.some(item => item.estadoRevision === 'CON_OBSERVACIONES');
    
    const anexosCargados: AnexoItem[] = (exp.anexos || []).map((a: AnexoAdjuntoDTO) => ({
      id: a.id.toString(),
      nombre: a.nombreAnexo,
      nombreArchivoExistente: a.nombreOriginal,
      rutaArchivoExistente: a.rutaArchivo,
      observacion: a.observacionAuditor || '',
      observacionAuditor: a.observacionAuditor || '',
      estadoRevision: a.estadoRevision,
      urlDescarga: a.rutaArchivo ? `${this.apiArchivosUrl}/pdf/${a.rutaArchivo}` : undefined
    }));
    
    this.anexos.set(anexosCargados);
    const tieneObservacionesAnex: boolean = anexosCargados.some(item => item.observacion  != '');

    this.conObservaciones = tieneObservaciones || tieneObservacionesAnex;

  }

  onFileSelected(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.asignarArchivoRequisito(input.files[0], index);
    }
  }

  onDrop(event: DragEvent, index: number): void {
    event.preventDefault();
    this.dragOverIndex.set(null);
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.asignarArchivoRequisito(event.dataTransfer.files[0], index);
    }
  }

  onDragOver(event: DragEvent, index: number): void {
    event.preventDefault();
    this.dragOverIndex.set(index);
  }

  onDragLeave(index: number): void {
    if (this.dragOverIndex() === index) {
      this.dragOverIndex.set(null);
    }
  }


  private esRequisitoPdf(nombre: string): boolean {
    if (!nombre) return false;
    const n = nombre.toLowerCase();
    return n.includes('carta') || n.includes('semblanza') || n.includes('currículum') || n.includes('curriculum');
  }

 
  getAcceptTypes(nombre: string): string {
    if (this.esRequisitoPdf(nombre)) {
      return 'application/pdf';
    }
    return '.xlsx, .xls, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/vnd.ms-excel';
  }

  private asignarArchivoRequisito(file: File, index: number): void {
    if (file.size > MAX_FILE_SIZE_BYTES) {
      const tamanoMB = (file.size / (1024 * 1024)).toFixed(2);
      this.alertService.mostrarAlerta(
        'Archivo demasiado grande',
        `El archivo pesa ${tamanoMB} MB. El tamaño máximo permitido es de 20 MB.`,
        'warning',
        'Aceptar',
        '#10312B'
      );
      return;
    }

    const requisito = this.documentosRequisitos()[index];
    const debeSerPdf = this.esRequisitoPdf(requisito.nombre);

    const ext = file.name.split('.').pop()?.toLowerCase();

    if (debeSerPdf) {
      if (file.type !== 'application/pdf' && ext !== 'pdf') {
        this.alertService.mostrarAlerta(
          'Formato Inválido',
          'Este documento únicamente acepta archivos en formato PDF (.pdf).',
          'warning',
          'Aceptar',
          '#10312B'
        );
        return;
      }
    } else {
      const esExcel = ext === 'xlsx' || ext === 'xls' ||
        file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
        file.type === 'application/vnd.ms-excel';

      if (!esExcel) {
        this.alertService.mostrarAlerta(
          'Formato Inválido',
          'Este documento únicamente acepta plantillas de Excel (.xlsx, .xls).',
          'warning',
          'Aceptar',
          '#10312B'
        );
        return;
      }
    }

    const lista = [...this.documentosRequisitos()];
    lista[index].archivo = file;
    this.documentosRequisitos.set(lista);
  }

  quitarArchivo(index: number): void {
    const lista = this.documentosRequisitos();
    if (lista[index]) {
      lista[index].archivo = undefined;
      lista[index].nombreArchivoExistente = undefined;
    }
  }

  private generarUUID(): string {
      if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
      }
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
      });
    }

    agregarAnexo(): void {
      if (this.anexos().length >= 10) {
        this.alertService.mostrarAlerta(
          'Límite alcanzado', 
          'Solo se permite un máximo de 10 archivos anexos.', 
          'info', 
          'Aceptar', 
          '#10312B'
        );
        return;
      }
      const nuevo: AnexoItem = {
        id: this.generarUUID() 
      };
      this.anexos.set([...this.anexos(), nuevo]);
    }

  async eliminarAnexo(idAnx: string): Promise<void> {
    const exp = this.expediente();
    const conv = this.convocatoria();
    const anexosActuales = this.anexos();

    const idAnexoNum = Number(idAnx);
    const anexoABorrar = anexosActuales.find(a => a.id === idAnx);

    if (!anexoABorrar?.rutaArchivoExistente && isNaN(idAnexoNum)) {
      this.anexos.set(anexosActuales.filter(a => a.id !== idAnx));
      return;
    }

    const confirmacion = await this.alertService.confirmarAccion(
      '¿Eliminar anexo?',
      'Esta acción eliminará permanentemente el archivo adjunto.',
      'Sí, eliminar',
      'Cancelar'
    );

    if (!confirmacion) return;

    try {
      this.alertService.mostrarLoader('Eliminando anexo...');

      const expedienteActualizado = await firstValueFrom(
        this.postulacionService.eliminarAnexoOpcional(exp!.id, idAnexoNum)
      );

      this.expediente.set(expedienteActualizado);

      if (conv) {
        this.inicializarExpediente(conv, expedienteActualizado);
      }

      this.alertService.cerrarLoader();
      this.alertService.mostrarAlertaConTimer(
        'Anexo eliminado',
        'El archivo anexo ha sido removido correctamente.',
        1500,
        'success'
      );
    } catch (error) {
      this.alertService.cerrarLoader();
      console.error('Error al eliminar el anexo:', error);
      this.alertService.mostrarAlerta(
        'Error',
        'No se pudo eliminar el anexo. Inténtalo de nuevo.',
        'error',
        'Aceptar',
        '#9b2247'
      );
    }
  }

  onAnexoSelected(event: Event, id: string): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.asignarArchivoAnexo(input.files[0], id);
    }
  }

  onAnexoDrop(event: DragEvent, id: string): void {
    event.preventDefault();
    this.dragOverAnexoId.set(null);
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.asignarArchivoAnexo(event.dataTransfer.files[0], id);
    }
  }

  private asignarArchivoAnexo(file: File, id: string): void {
    if (file.size > MAX_FILE_SIZE_BYTES) {
      const tamanoMB = (file.size / (1024 * 1024)).toFixed(2);
      this.alertService.mostrarAlerta(
        'Archivo demasiado grande',
        `El anexo pesa ${tamanoMB} MB. El tamaño máximo permitido es de 20 MB.`,
        'warning',
        'Aceptar',
        '#10312B'
      );
      return;
    }

    const ext = file.name.split('.').pop()?.toLowerCase();
    const esPermitido = ext === 'pdf' || ext === 'xlsx' || ext === 'xls';

    if (!esPermitido) {
      this.alertService.mostrarAlerta(
        'Formato Inválido',
        'El anexo debe ser un archivo PDF (.pdf) o Excel (.xlsx, .xls).',
        'warning',
        'Aceptar',
        '#10312B'
      );
      return;
    }

    const lista = this.anexos().map(a => a.id === id ? { ...a, archivo: file } : a);
    this.anexos.set(lista);
  }

  puedeCargarRequisito(req: any): boolean {
    if (this.estaRechazadoTotal()) {
      return false;
    }

    const estadoGeneral = this.estadoExpediente();
    if (estadoGeneral === 'BORRADOR' || estadoGeneral === 'CON_OBSERVACIONES') {
      if (req.estadoRevision === 'APROBADO') {
        return false;
      }
      return true;
    }

    return false;
  }

  async guardarBorrador(): Promise<void> {
    const exp = this.expediente();
    const conv = this.convocatoria();
    if (!exp || !conv) return;

    this.alertService.mostrarLoader('Guardando avance de tus documentos...');
    this.guardando.set(true);

    try {
      let expedienteActualizado: PostulacionResponseDTO = { ...exp };

      for (const req of this.documentosRequisitos()) {
        if (req.archivo instanceof File && req.idRequisito) {
          expedienteActualizado = await firstValueFrom(
            this.postulacionService.subirDocumentoRequisito(
              exp.id,
              Number(req.idRequisito),
              req.archivo
            )
          );
        }
      }

      for (const anx of this.anexos()) {
        const tieneArchivoFisico = anx.archivo instanceof File;

        if (tieneArchivoFisico || (anx.reemplazar && tieneArchivoFisico)) {
          
          expedienteActualizado = await firstValueFrom(
            this.postulacionService.subirAnexoOpcional(
              exp.id,
              anx.archivo!, 
              parseInt(anx.id) || 0,
              anx.nombre || `Anexo_${anx.id}`
            )
          );

        }
      }

      this.expediente.set(expedienteActualizado);
      this.inicializarExpediente(conv, expedienteActualizado);

      this.alertService.cerrarLoader();
      this.alertService.mostrarAlertaConTimer(
        'Borrador Guardado',
        'Tus archivos y anexos se han guardado con éxito.',
        1500,
        'success'
      );
    } catch (error) {
      this.alertService.cerrarLoader();
      console.error('Error al guardar borrador:', error);
      this.alertService.mostrarAlerta(
        'Error',
        'No se pudieron guardar algunos anexos o requisitos.',
        'error',
        'Aceptar',
        '#9b2247'
      );
    } finally {
      this.guardando.set(false);
    }
  }

  async enviarPostulacion(): Promise<void> {
    
    const exp = this.expediente();
    if (!exp) return;

    const faltantes = this.documentosRequisitos().filter(r => !r.archivo && !r.nombreArchivoExistente);
    if (faltantes.length > 0) {
      this.alertService.mostrarAlerta(
        'Documentos Incompletos',
        `Debes adjuntar todos los documentos requeridos.`,
        'warning',
        'Aceptar',
        '#10312B'
      );
      return;
    }
    
    const tieneAlMenosUnArchivo = this.anexos().some(anx => anx.archivo instanceof File || !!anx.rutaArchivoExistente || !!anx.nombreArchivoExistente);

    if (!tieneAlMenosUnArchivo) {
      this.alertService.mostrarAlerta(
        'Documentos Incompletos',
        'Debes adjuntar por lo menos un documento probatorio de todo lo descrito en tu CV.',
        'warning',
        'Aceptar',
        '#10312B'
      );
      return;
    }

    const confirmado = await this.alertService.confirmarAccion(
      '¿Enviar Expediente a Revisión?',
      `Se registrará formalmente tu postulación bajo el folio ${exp.folio}. Una vez enviado, no podrás modificar los archivos.`,
      'Sí, enviar formalmente',
      'Cancelar'
    );

    if (!confirmado) return;

    this.alertService.mostrarLoader('Enviando expediente...');
    this.guardando.set(true);

    try {
      await this.guardarBorrador();

      const resultadoFinal = await firstValueFrom(
        this.postulacionService.enviarExpediente(exp.id)
      );

      this.expediente.set(resultadoFinal);
      this.estadoExpediente.set('ENVIADO');

      this.alertService.cerrarLoader();
      this.alertService.mostrarAlerta(
        '¡Expediente Enviado!',
        `Tu postulación ha sido enviada exitosamente con el Folio: ${resultadoFinal.folio}.`,
        'success',
        'Aceptar',
        '#10312B'
      );

      this.router.navigate(['postulante/convocatoria-vigente/'+this.idParam]);
    } catch (error: any) {
      this.alertService.cerrarLoader();
      console.error(error);
      this.alertService.mostrarAlerta(
        'Error',
        error?.error?.error || 'No fue posible formalizar el envío del expediente.',
        'error',
        'Aceptar',
        '#9b2247'
      );
    } finally {
      this.guardando.set(false);
    }
  }

  obtenerUrlImagen(ruta?: string): string {
    if (!ruta) return '';
    const nombreLimpio = ruta.replace(/^.*[\\\/]/, '');
    return `${this.apiArchivosUrl}/img/${nombreLimpio}`;
  }

  volver(): void {
    this.router.navigate(['/postulante/convocatoria-vigente/' + this.idParam]);
  }

  async logout(): Promise<void> {
    const confirm = await this.alertService.confirmarAccion(
      'Cerrar Sesión',
      '¿Deseas salir del sistema de convocatorias?',
      'Sí, Salir'
    );

    if (!confirm) return;

    localStorage.clear();
    sessionStorage.clear();
    this.router.navigate(['/login']);
  }

  tieneRevisionesPreviasMetodo(): boolean {
    const documentos = this.documentosRequisitos();

    if (this.estadoExpediente() != 'ACEPTADO' && this.estadoExpediente() != 'RECHAZADO') {
      let hayObservaciones = documentos.some(doc => doc.estadoRevision === 'CON_OBSERVACIONES');

      if (this.estadoExpediente() === 'BORRADOR') {
        return true;
      } else if (this.estadoExpediente() === 'ENVIADO' && hayObservaciones) {
        return true;
      } else if (this.estadoExpediente() === 'ENVIADO' && !hayObservaciones) {
        return false;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  puedeEditarAnexo(anx: AnexoItem): boolean {
    if (this.estaRechazadoTotal()) {
      return false;
    }

    if (anx.reemplazar || anx.observacion!='') {
      return true;
    }

    return false;
  }

  quitarArchivoAnexo(idAnx: string): void {
    const lista = this.anexos().map(a => {
      if (a.id === idAnx) {
        return {
          ...a,
          archivo: undefined,
          nombreArchivoExistente: undefined,
          rutaArchivoExistente: undefined,
          reemplazar: true
        };
      }
      return a;
    });
    this.anexos.set(lista);
  }

  documentosPendientes(){

    this.postulacionService.revisaEstadoObservaciones(this.expediente()!.id).subscribe({
      next: (data)=>{
        this.hayPendientes.set(data.tieneObservaciones); 
      },
      error: (err) =>{

      }
    });

  }
  
}