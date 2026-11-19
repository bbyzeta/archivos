import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DictamenReporte } from '../models/dictamen.model';
import { environment } from '../../environments/environment.development';


@Injectable({
  providedIn: 'root'
})
export class DictamenService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl +'/dictamenes';

  obtenerDictamenes(idConvocatoria?: number | null, idJuez?: number | null, termino?: string): Observable<DictamenReporte[]> {
    let params = new HttpParams();
    if (idConvocatoria) params = params.set('idConvocatoria', idConvocatoria.toString());
    if (idJuez) params = params.set('idJuez', idJuez.toString());
    if (termino) params = params.set('termino', termino);

    return this.http.get<DictamenReporte[]>(this.apiUrl, { params });
  }
}