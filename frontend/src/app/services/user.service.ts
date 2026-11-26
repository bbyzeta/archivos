import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChangePasswordRequest, CreateUserRequest, UpdateUserRequest, User } from '../models/user.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/users`;

  list(): Observable<User[]> {
    return this.http.get<User[]>(this.url);
  }

  create(request: CreateUserRequest): Observable<User> {
    return this.http.post<User>(this.url, request);
  }

  /** Edita el nombre completo y el rol. */
  update(id: string, request: UpdateUserRequest): Observable<User> {
    return this.http.patch<User>(`${this.url}/${id}`, request);
  }

  setActive(id: string, active: boolean): Observable<User> {
    return this.http.patch<User>(`${this.url}/${id}/active`, { active });
  }

  /** Restablece la contraseña de otro usuario (solo administradores). */
  resetPassword(id: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.url}/${id}/password`, { newPassword });
  }

  /** Cambia la contraseña del usuario autenticado. */
  changeOwnPassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.put<void>(`${this.url}/me/password`, request);
  }
}
