export interface JuradoItem {
  idJurado: number;
  gradoAcademico: string;
  nombres: string;
  apellidos: string;
  rfc?: string;
  telefono: string;
  username: string; 
  institucionAdscripcion: string;
  cargoActual: string;
  areaConocimiento: string;
  orcidUrl?: string;
  lineasInvestigacion: string;
  aceptoConflictoInteres: boolean;
  aceptoConfidencialidad: boolean;
  activo: boolean;
  fechaRegistro?: string;
}