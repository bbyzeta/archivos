import { Injectable, Signal, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';
import { ExpedienteAuditoriaDTO, ExpedienteBandejaItem, PageResponse } from '../models/auditor.model';
import { ConvocatoriaJuradoDTO } from '../models/convocatorias.model';

@Injectable({
  providedIn: 'root'
})
export class JuradoExpedienteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/jurado/expedientes`;



  listarExpedientesParaAuditoria(
    pagina: number = 0,
    tamanio: number = 10,
    busqueda: string = '',
    idConvocatoria: number | null = null,
    estado: string = '',
    idsCategorias: number[] = []
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

    if (idsCategorias && idsCategorias.length > 0) {
      params = params.set('idsCategorias', idsCategorias.join(','));
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

  getIdCategoria(idUsuario:number): Observable<any>{
    return this.http.get<any>(`${this.apiUrl}/convocatoria/${idUsuario}`);
  }

  obtenerTotalEvaluacion(idUsuario: number, idConvocatoria: number): Observable<number> {
    const params = new HttpParams()
      .set('idUsuario', idUsuario.toString())
      .set('idConvocatoria', idConvocatoria.toString());

    return this.http.get<number>(`${this.apiUrl}/total-porcentaje`, { params });
  }

  esPostulantePropuesto(idConvocatoria: number, idUsuario: number, idJuez: number): Observable<number> {
    const params = new HttpParams()
      .set('idConvocatoria', idConvocatoria.toString())
      .set('idUsuario', idUsuario.toString())
      .set('idJuez', idJuez.toString());

    return this.http.get<number>(`${this.apiUrl}/propuesto`, { params });
  }
}