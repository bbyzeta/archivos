import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PostulacionRegistroDTO, PostulacionResponseDTO } from '../models/expediente.model';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class PostulacionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl+'/postulaciones';

  iniciarOObtenerBorrador(dto: PostulacionRegistroDTO): Observable<PostulacionResponseDTO> {
    return this.http.post<PostulacionResponseDTO>(`${this.baseUrl}/iniciar`, dto);
  }

  obtenerPorId(id: number): Observable<PostulacionResponseDTO> {
    return this.http.get<PostulacionResponseDTO>(`${this.baseUrl}/${id}`);
  }

  subirDocumentoRequisito(idPostulacion: number, idRequisito: number, archivo: File): Observable<PostulacionResponseDTO> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<PostulacionResponseDTO>(
      `${this.baseUrl}/${idPostulacion}/documentos/${idRequisito}`, 
      formData
    );
  }

  enviarExpediente(idPostulacion: number): Observable<PostulacionResponseDTO> {
    return this.http.patch<PostulacionResponseDTO>(`${this.baseUrl}/${idPostulacion}/enviar`, {});
  }

  subirAnexoOpcional(idPostulacion: number, archivo: File, idAnexo:number, nombreAnexo?: string): Observable<PostulacionResponseDTO> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    if (nombreAnexo) {
        formData.append('nombreAnexo', nombreAnexo);
    }
    return this.http.post<PostulacionResponseDTO>(`${this.baseUrl}/${idPostulacion}/anexos/${idAnexo}`, formData);
 }

 eliminarAnexoOpcional(idPostulacion: number, idAnexo: number): Observable<PostulacionResponseDTO> {
    return this.http.delete<PostulacionResponseDTO>(`${this.baseUrl}/${idPostulacion}/anexos/${idAnexo}`);
 }

 revisaEstadoObservaciones(id: number): Observable<any> {
    return this.http.get<PostulacionResponseDTO>(`${this.baseUrl}/pendientes/${id}`);
 }
}