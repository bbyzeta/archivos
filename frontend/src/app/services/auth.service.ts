import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse } from '../models/login.model';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.apiUrlAuth;

  readonly currentUser = signal<LoginResponse | null>(this.obtenerUsuarioAlmacenado());

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API_URL}/auth/login`, credentials).pipe(
      tap((res) => {
        console.log(res);
        sessionStorage.setItem('token', res.token);
        sessionStorage.setItem('userName', res.userName);
        sessionStorage.setItem('idUsuario', res.idUsuario.toString());
        sessionStorage.setItem('rol', res.rol);
        sessionStorage.setItem('usuario', JSON.stringify(res));

        this.currentUser.set(res);
      })
    );
  }

  logout(): void {
    sessionStorage.clear();
    this.currentUser.set(null);
  }

  isLoggedIn(): boolean {
    return !!sessionStorage.getItem('token');
  }

  getUsuarioLogueado(): LoginResponse | null {
    return this.currentUser() || this.obtenerUsuarioAlmacenado();
  }

  getToken(): string | null {
    return sessionStorage.getItem('token');
  }

  getUserName(): string {
    return sessionStorage.getItem('userName') || '';
  }

  getRol(): string {
    return sessionStorage.getItem('rol') || '';
  }

  getIdUsuario(): number | null {
    const id = sessionStorage.getItem('idUsuario');
    return id ? Number(id) : null;
  }

  private obtenerUsuarioAlmacenado(): LoginResponse | null {
    const raw = sessionStorage.getItem('usuario');
    if (!raw) return null;
    try {
      return JSON.parse(raw) as LoginResponse;
    } catch {
      return null;
    }
  }
}