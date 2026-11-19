import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.apiUrl;
  
  registrar(datos: any): Observable<any> {
    return this.http.post<any>(this.API_URL+"/admin/convocatorias", datos);
  }

  actualizar(id: number, formData: FormData): Observable<any> {
    return this.http.put(`${this.API_URL}/admin/convocatorias/${id}`, formData);
  }
}