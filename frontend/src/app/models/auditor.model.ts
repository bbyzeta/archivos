import { EstadoExpediente } from "./expediente.model";

export type EstadoRevisionDoc = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'CON_OBSERVACIONES';

export interface ItemRevisionDocumento {
  idDocumento: number;
  nombre: string;
  esAnexo: boolean;
  nombreOriginal: string;
  rutaArchivo: string;
  estadoRevision: EstadoRevisionDoc;
  observacionAuditor?: string;
  corregido:number;
}

export interface ExpedienteAuditoriaDTO {
  idExpediente: number;
  folio: string;
  estadoExpediente: string;
  fechaEnvio: string;
  idPostulante: number;
  nombrePostulante: string;
  correoPostulante: string;
  curpPostulante: string;
  telefonoPostulante: string;
  institucionPostulante: string;
  idConvocatoria: number;
  tituloConvocatoria: string;
  periodoConvocatoria: number | string;
  fechaApertura: string;
  fechaCierre: string;
  rutaImagenPremio?: string;
  documentos: ItemRevisionDocumento[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;             
  numberOfElements?: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
}

export interface ExpedienteBandejaItem {
  idExpediente: number;
  folio: string;
  idConvocatoria: number;
  tituloConvocatoria: string;
  periodo: number | string;
  idPostulante: number;
  nombrePostulante: string;
  correoPostulante: string;
  curpPostulante: string;
  institucionPostulante?: string;
  estadoExpediente: EstadoExpediente; 
  totalDocumentos: number;
  documentosAprobados: number;
  fechaEnvio?: string;
  propuesto: number;
  totalPorcentaje: number;
}