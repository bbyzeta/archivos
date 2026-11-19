import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';
import { ConvocatoriaGeneralResponseDTO, ConvocatoriaHijaDTO } from '../models/convocatorias-general.model';


@Injectable({
  providedIn: 'root'
})
export class ConvocatoriaGeneralService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
   private readonly apiArchivosUrl = environment.apiUrl + '/archivos/view';

  listarConvocatorias(): Observable<ConvocatoriaGeneralResponseDTO[]> {
    return this.http.get<ConvocatoriaGeneralResponseDTO[]>(this.apiUrl + '/admin/convocatorias/general');
  }

  obtenerHijasPorCategoria(idCategoria: number): Observable<ConvocatoriaHijaDTO[]> {
    return this.http.get<ConvocatoriaHijaDTO[]>(`${this.apiUrl}/admin/convocatorias/categoria/${idCategoria}`);
  }

  obtenerImagenHeader(nombreImagen: string): Observable<Blob> {
    return this.http.get(`${this.apiArchivosUrl}/img/${nombreImagen}`, {
      responseType: 'blob'
    });
  }

  
}