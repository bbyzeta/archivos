import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Estado } from '../models/estado.model';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class EstadoService {

  private readonly API_URL = environment.apiUrl;
  

  constructor(private http: HttpClient) { }

  getEstados(): Observable<Estado[]> {
    return this.http.get<Estado[]>(this.API_URL + '/estados');
  }
}