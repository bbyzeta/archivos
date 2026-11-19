export interface CriterioEvaluado {
  idCriterio: number;
  nombreCriterio: string;
  factoresConsiderar: string;
  porcentajeMaximo: number;
  porcentajeAsignado: number;
  argumento:string;
}

export interface DictamenReporte {
  idConvocatoria: number;
  tituloConvocatoria: string;
  periodo: number;
  idJuez: number;
  nombreJuez: string;
  idPostulante: number;
  nombrePostulante: string;
  curpPostulante: string;
  porcentajeTotal: number;
  estadoEvaluacion: number;
  argumentoGeneral?: string;
  criterios: CriterioEvaluado[];
}