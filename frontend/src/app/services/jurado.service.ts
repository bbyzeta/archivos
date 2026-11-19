import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JuradoItem } from '../models/jurado.models';
import { environment } from '../../environments/environment.development';
import { ArgumentoEvaluacion, CriterioEvaluacion } from '../models/jurado.model';
import { Evaluacion } from '../models/evaluacion.model';

@Injectable({
  providedIn: 'root'
})
export class JuradoService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/admin/jurados';
  private apiUrlJuradoConvocatoria = environment.apiUrl + '/admin/convocatorias';
  private apiUrlCriterios = environment.apiUrl + '/criterios';
  private apiUrlEvaluacion = environment.apiUrl + '/evaluacion/postulante';

  listarPaginado(pagina: number, tamanio: number, termino: string): Observable<any> {
    let params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', tamanio.toString());
    
    if (termino.trim()) {
      params = params.set('filtro', termino.trim());
    }

    return this.http.get<any>(this.apiUrl, { params });
  }

  crear(jurado: Partial<JuradoItem>): Observable<JuradoItem> {
    return this.http.post<JuradoItem>(this.apiUrl, jurado);
  }

  actualizar(id: number, jurado: Partial<JuradoItem>): Observable<JuradoItem> {
    return this.http.put<JuradoItem>(`${this.apiUrl}/${id}`, jurado);
  }

  cambiarEstado(id: number, activo: boolean): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/estado`, { activo });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  obtenerJuradosAsignados(idConvocatoria: number): Observable<number[]> {
    return this.http.get<number[]>(this.apiUrlJuradoConvocatoria+`/${idConvocatoria}/jurados/ids`);
  }

  asignarJuradosAConvocatoria(idConvocatoria: number, juradoIds: number[]): Observable<void> {
    return this.http.post<void>(this.apiUrlJuradoConvocatoria+`/${idConvocatoria}/jurados`, { juradoIds });
  }

  obtenerJuradosCompletosPorConvocatoria(idConvocatoria: number): Observable<JuradoItem[]> {
    return this.http.get<JuradoItem[]>(this.apiUrlJuradoConvocatoria+`/${idConvocatoria}/jurados/detalle`);
  }

  obtenerCriterios(idConvocatoria: number, idUsuario:number): Observable<CriterioEvaluacion[]> {
    return this.http.get<CriterioEvaluacion[]>(`${this.apiUrlCriterios}/convocatoria/${idConvocatoria}/${idUsuario}`);
  }
  
  guardarEvaluacionPostulante(listaEvaluacion:Evaluacion[]): Observable<Evaluacion[]> {
    return this.http.post<Evaluacion[]>(this.apiUrlEvaluacion, listaEvaluacion);
  }
  
  guardarArgumentosPostulante(listaArgumentos:ArgumentoEvaluacion[]): Observable<any[]>{
    return this.http.post<Evaluacion[]>(this.apiUrlEvaluacion+"/masivo", listaArgumentos);
  }
 
  validaPermiteProponer(dto:ArgumentoEvaluacion): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrlEvaluacion}/valida/propuesta`,dto);
  }
}