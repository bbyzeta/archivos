/** Color por defecto del icono de carpeta (dorado institucional). */
export const DEFAULT_FOLDER_COLOR = '#a57f2c';

export interface FolderColorOption {
  name: string;
  value: string;
}

/** Paleta de colores para carpetas: tonos institucionales más algunos neutros y de apoyo. */
export const FOLDER_COLORS: FolderColorOption[] = [
  { name: 'Guinda', value: '#9b2247' },
  { name: 'Vino', value: '#611232' },
  { name: 'Verde', value: '#1e5b4f' },
  { name: 'Verde oscuro', value: '#002f2a' },
  { name: 'Dorado', value: '#a57f2c' },
  { name: 'Azul', value: '#2f5d8a' },
  { name: 'Naranja', value: '#c2641a' },
  { name: 'Gris', value: '#6b7a76' }
];
