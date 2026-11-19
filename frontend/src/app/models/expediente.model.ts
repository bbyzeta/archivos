export type EstadoExpediente = 'BORRADOR' | 'ENVIADO' | 'CON_OBSERVACIONES' | 'RECHAZADO' | 'ACEPTADO' | 'PENDIENTE';

export interface DocumentoRequisitoItem {
  idRequisito?: number;
  nombre: string;
  esAnexo: boolean;
  archivo?: File;
  nombreArchivoExistente?: string;
  rutaArchivoExistente?: string; 
  urlDescarga?: string;
  observacion?: string;
  observacionAuditor?: string; 
  estadoRevision?: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'OBSERVADO' | 'CON_OBSERVACIONES' | 'NO CARGADO';
  rechazadoPorObservacion?: boolean;
}

export interface AnexoItem {
  id: string;
  nombre?: string;
  archivo?: File;
  nombreArchivoExistente?: string;
  rutaArchivoExistente?: string;
  urlDescarga?: string;
  observacion?: string;         
  observacionAuditor?: string; 
  estadoRevision?: string; 
  reemplazar?: boolean;
}

export interface DocumentoAdjuntoDTO {
  id?: number;
  idRequisito: number;
  nombreRequisito?: string;
  esAnexo?: boolean;
  nombreOriginal: string;
  rutaArchivo: string;
  fechaCarga?: string;
  observacionAuditor?: string;         
  estadoRevision?: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'OBSERVADO' | 'CON_OBSERVACIONES'; 
}

export interface PostulacionResponseDTO {
  id: number;
  idConvocatoria: number;
  idUsuario: number;
  folio: string;
  estado: EstadoExpediente;
  tituloProyecto: string;
  resumenEjecutivo?: string;
  motivoRechazo?: string;        
  fechaCreacion: string;
  fechaEnvio?: string;
  documentos: DocumentoAdjuntoDTO[];
  anexos?: AnexoAdjuntoDTO[];
}

export interface PostulacionRegistroDTO {
  idConvocatoria: number;
  idUsuario: number;
  tituloProyecto: string;
  resumenEjecutivo?: string;
}

export interface AnexoAdjuntoDTO {
  id: number;
  idConvocatoria: number;
  nombreAnexo: string;
  nombreOriginal: string;
  rutaArchivo: string;
  fechaCarga?: string;
  observacionAuditor?: string;         
  estadoRevision?: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'OBSERVADO' | 'CON_OBSERVACIONES';          
}






export interface DocumentoRequisito {
  idRequisito: number;
  nombre: string;
  obligatorio: boolean;
  archivo?: File;
  nombreArchivoExistente?: string;
  urlDescarga?: string;
  estadoRevision?: 'PENDIENTE' | 'APROBADO' | 'OBSERVADO' | 'RECHAZADO';
  rechazadoPorObservacion?: boolean;
  observacionAuditor?: string;
  observacion?: string;
}

export interface DocumentoAnexo {
  id: string;
  archivo?: File;
  nombreArchivoExistente?: string;
  urlDescarga?: string;
  observacionAuditor?: string;
  observacion?: string;
}

export interface Convocatoria {
  id: number;
  titulo: string;
  subtitulo?: string;
  periodo: string;
  fechaApertura: string;
  fechaCierre: string;
  rutaImagenHeader?: string;
}

