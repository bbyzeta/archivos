export interface RequisitoConvocatoria {
  id?: string | number;
  nombre: string;
  esAnexo: boolean;
}

export interface GanadorHistorico {
  id?: number;
  nombreGanador: string;
  anio: number;
}

export interface ConvocatoriaItem {
  id: number;
  titulo: string;
  subtitulo?: string;
  fechaApertura: string; 
  fechaCierre: string;
  activa: boolean;
  contenidoBlog?: string;
  imagenHeaderUrl?: string;
  documentoBasesUrl?: string;
  requisitos: RequisitoConvocatoria[];
  ganadoresHistoricos: GanadorHistorico[];
}

export interface RequisitoDTO {
  id_requisito?: number;
  idRequisito?: number;
  id?: number;
  nombre: string;
  esAnexo: boolean;
  porcentajeMinimo:number;
  porcentajeMaximo:number;
}

export interface GanadorDTO {
  nombreGanador: string;
  anio: number;
}

export interface ConvocatoriaResponseDTO {
  id: number;
  titulo: string;
  subtitulo?: string;
  tituloBlog: string;
  fechaApertura: string;
  fechaCierre: string;
  contenidoBlog?: string;
  rutaImagenHeader?: string;
  rutaPdfBases?: string;
  periodo?: number;
  activa: boolean;
  requisitos: RequisitoDTO[];
  ganadoresHistoricos: GanadorDTO[];
  idGrupoPremio:number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ConvocatoriaJuradoDTO{
  id:number;
  idJurado:number
}