import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class RecuperarService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl + "/cuenta";

  recuperarCuenta(correo:string): Observable<any> {
    const params = new HttpParams().set('correo', correo);  
    return this.http.post<any>(`${this.baseUrl}/recuperar`, null, { params });
  }

  restablecerCuenta(newPassword:string, token:string): Observable<any> {
    var body = {'newPassword': newPassword, 'token': token}
    return this.http.post<any>(`${this.baseUrl}/restablecer`, body);
  }

}