import { signal } from "@angular/core";
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
}

export interface CriterioEvaluacion {
  idCriterio: number;
  nombreCriterio: string;
  factoresConsiderar: string;
  porcentajeMaximo: number;
  porcentajeAsignado: number;
  criterioEvaluado: number;
  argumentos?: string;
}

export interface ArgumentoEvaluacion {
  idArgumento: number;
  idConvocatoria: number;
  idCriterio: number;
  idUsuario: number;
  idJuez: number;
  argumento: string;
}


 