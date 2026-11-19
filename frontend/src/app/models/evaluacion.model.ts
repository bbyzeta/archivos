export interface Evaluacion {
  idEvaluacion:number;
  idExpediente:number;
  idCriterio:number;
  idUsuario:number;
  porcentajeAsignado:number;
  criterioEvaluado:number;
  fechaEvaluacion:string;
}

export interface ModalEvaluacionData {
  nombrePostulante: string;
  criterios: Evaluacion[];
}