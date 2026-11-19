import { SafeUrl } from "@angular/platform-browser";


export interface ConvocatoriaGeneralResponseDTO {
  id: number;
  titulo: string;
  descripcion?: string;
  activa: boolean;
}

export interface ConvocatoriaHijaDTO {
  id: number;
  titulo: string;
  subtitulo?: string;
  rutaImagenHeader?: string;
  activa: boolean;
}

export interface ConvocatoriaConImagen extends ConvocatoriaHijaDTO {
  imagenUrl?: SafeUrl;
}