import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConvocatoriaResponseDTO, PageResponse } from '../models/convocatorias.model';
import { environment } from '../../environments/environment.development';


@Injectable({
  providedIn: 'root'
})
export class ConvocatoriaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl + '/admin/convocatorias';
  private readonly publicApiUrl = environment.apiUrl + '/convocatorias';

  listar(pagina: number = 0, tamanio: number = 5, busqueda: string = ''): Observable<PageResponse<ConvocatoriaResponseDTO>> {
    let params = new HttpParams()
      .set('pagina', pagina.toString())
      .set('tamanio', tamanio.toString());

    if (busqueda && busqueda.trim().length > 0) {
      params = params.set('busqueda', busqueda.trim());
    }

    return this.http.get<PageResponse<ConvocatoriaResponseDTO>>(this.apiUrl, { params });
  }

  obtenerPorId(id: number): Observable<ConvocatoriaResponseDTO> {
    return this.http.get<ConvocatoriaResponseDTO>(`${this.apiUrl}/${id}`);
  }

  crear(formData: FormData): Observable<any> {
    return this.http.post(this.apiUrl, formData);
  }

  actualizar(id: number, formData: FormData): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, formData);
  }

  toggleActiva(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/toggle-activa`, {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  obtenerConvocatoriaActiva(): Observable<ConvocatoriaResponseDTO> {
    return this.http.get<ConvocatoriaResponseDTO>(`${this.apiUrl}/activa`);
  }
  
  obtenerConvocatoriaPorId(id:string): Observable<ConvocatoriaResponseDTO> {
    return this.http.get<ConvocatoriaResponseDTO>(`${this.apiUrl}/${id}`);
  }

}