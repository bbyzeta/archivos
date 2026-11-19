import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class ActivarService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  activarUsuario(token:string): Observable<any> {
    const params = new HttpParams().set('token', token);  
    return this.http.post<any>(`${this.baseUrl}/activar`, null, { params });
  }

}