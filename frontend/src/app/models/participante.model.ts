export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface DatosInstitucionalesDTO {
  idInstitucional?: number;
  idEstado: number;
  institucion: string;
  cargo: string;
}

export interface ParticipanteDTO {
  idParticipante: number;
  nombres: string;
  apellidos: string;
  curp: string;
  telefono: string;
  username: string;
  activo: boolean;
  aceptoTerminos: boolean;
  fechaRegistro: string;
  datosInstitucionales?: DatosInstitucionalesDTO;
}

export interface ParticipanteRequestDTO {
  nombres: string;
  apellidos: string;
  curp: string;
  telefono: string;
  username: string;
  password?: string;
  activo: boolean;
  datosInstitucionales: DatosInstitucionalesDTO;
}

export interface PageParticipanteResponse {
  content: ParticipanteDTO[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}


export interface RequisitoConvocatoria {
  idRequisito?: number;
  nombre: string;
  descripcion?: string;
  esAnexo: boolean;
  pesoMaximoMb?: number;
}

export interface GanadorHistorico {
  anio: number;
  nombreGanador: string;
  proyecto?: string;
}

export interface ConvocatoriaDetalle {
  id: number;
  titulo: string;
  subtitulo?: string;
  periodo: string;
  fechaApertura: string;
  fechaCierre: string;
  activa: boolean;
  tituloBlog?: string;
  contenidoBlog?: string;
  rutaImagenHeader?: string;
  rutaPdfBases?: string;
  requisitos: RequisitoConvocatoria[];
  ganadoresHistoricos: GanadorHistorico[];
}

export interface titulos{
  idTitutlo:number;
  abr:string;
  valorTitulo:string;
  descripcionTitulo:string;
}

export const listaTitulos:titulos[] =[
  {
    idTitutlo:1,
    abr:"Dr.",
    valorTitulo:"Doctor(a)",
    descripcionTitulo:"Doctor(a)"
  },
  {
    idTitutlo:2,
    abr:"Mtr.",
    valorTitulo:"Maestro(a)",
    descripcionTitulo:"Maestro(a)"
  },
  {
    idTitutlo:3,
    abr:"Ing.",
    valorTitulo:"Ingeniero(a)",
    descripcionTitulo:"Ingeniero(a)"
  },
  {
    idTitutlo:4,
    abr:"Lic.",
    valorTitulo:"Licenciado(a)",
    descripcionTitulo:"Licenciado(a)"
  },
]