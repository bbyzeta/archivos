import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';
import { ExpedienteAuditoriaDTO, ExpedienteBandejaItem, PageResponse } from '../models/auditor.model';

@Injectable({
  providedIn: 'root'
})
export class AuditorService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/auditor/expedientes`;



  listarExpedientesParaAuditoria(
    pagina: number = 0,
    tamanio: number = 10,
    busqueda: string = '',
    idConvocatoria: number | null = null,
    estado: string = ''
  ): Observable<PageResponse<ExpedienteBandejaItem>> {
    let params = new HttpParams()
      .set('pagina', pagina.toString())
      .set('tamanio', tamanio.toString());

    if (busqueda && busqueda.trim().length > 0) {
      params = params.set('busqueda', busqueda.trim());
    }

    if (idConvocatoria !== null && idConvocatoria !== undefined) {
      params = params.set('idConvocatoria', idConvocatoria.toString());
    }

    if (estado && estado.trim().length > 0 && estado !== 'TODOS') {
      params = params.set('estado', estado.trim());
    }

    return this.http.get<PageResponse<ExpedienteBandejaItem>>(this.apiUrl, { params });
  }


  obtenerExpedienteParaAuditoria(idExpediente: number): Observable<ExpedienteAuditoriaDTO> {
    return this.http.get<ExpedienteAuditoriaDTO>(`${this.apiUrl}/${idExpediente}`);
  }

  actualizarDictamenDocumento(
    idExpediente: number,
    idDocumento: number,
    body: { estado: string; observacion?: string }
  ): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${idExpediente}/documentos/${idDocumento}`, body);
  }


  dictaminarExpedienteAuditor(
    idExpediente: number,
    body: { resultado: string; comentarios: string }
  ): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${idExpediente}/dictamen`, body);
  }

}