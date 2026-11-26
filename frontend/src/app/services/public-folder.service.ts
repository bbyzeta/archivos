import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PublicFolder } from '../models/folder.model';
import { environment } from '../../environments/environment';

/** Consulta pública de carpetas (no requiere sesión). */
@Injectable({ providedIn: 'root' })
export class PublicFolderService {
  private readonly http = inject(HttpClient);

  get(slug: string): Observable<PublicFolder> {
    return this.http.get<PublicFolder>(`${environment.apiUrl}/public/${encodeURIComponent(slug)}`);
  }
}
