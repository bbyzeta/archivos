import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';
import { PageResponse, ParticipanteDTO, ParticipanteRequestDTO } from '../models/participante.model';

@Injectable({
  providedIn: 'root'
})
export class ParticipanteService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.apiUrl;
  private readonly apiUrl = environment.apiUrl+"/participantes";
  
  registrar(datos: any): Observable<any> {
    return this.http.post<any>(this.API_URL+"/registro", datos);
  }

  listar(page: number, size: number, buscar: string = ''): Observable<PageResponse<ParticipanteDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (buscar.trim()) {
      params = params.set('busqueda', buscar.trim());
    }

    return this.http.get<PageResponse<ParticipanteDTO>>(this.apiUrl, { params });
  }

  obtenerPorId(id: number): Observable<ParticipanteDTO> {
    return this.http.get<ParticipanteDTO>(`${this.apiUrl}/${id}`);
  }

  crear(dto: ParticipanteRequestDTO): Observable<ParticipanteDTO> {
    return this.http.post<ParticipanteDTO>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: ParticipanteRequestDTO): Observable<ParticipanteDTO> {
    return this.http.put<ParticipanteDTO>(`${this.apiUrl}/${id}`, dto);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  toggleActivo(idUsuario: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${idUsuario}/toggle-activo`, {});
  }
}