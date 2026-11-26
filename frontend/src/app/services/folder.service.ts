import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Folder, FolderFile, FolderRequest } from '../models/folder.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FolderService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/folders`;

  list(): Observable<Folder[]> {
    return this.http.get<Folder[]>(this.url);
  }

  get(id: string): Observable<Folder> {
    return this.http.get<Folder>(`${this.url}/${id}`);
  }

  create(request: FolderRequest): Observable<Folder> {
    return this.http.post<Folder>(this.url, request);
  }

  listFiles(folderId: string): Observable<FolderFile[]> {
    return this.http.get<FolderFile[]>(`${this.url}/${folderId}/files`);
  }

  /** Sube un archivo emitiendo eventos para poder mostrar el progreso. */
  uploadFile(folderId: string, file: File, relativePath = ''): Observable<HttpEvent<FolderFile>> {
    const body = new FormData();
    body.append('file', file, file.name);
    if (relativePath) body.append('relativePath', relativePath);
    return this.http.post<FolderFile>(`${this.url}/${folderId}/files`, body, {
      observe: 'events',
      reportProgress: true
    });
  }

  listDirectories(folderId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.url}/${folderId}/directories`);
  }

  createDirectory(folderId: string, path: string): Observable<{ path: string }> {
    return this.http.post<{ path: string }>(`${this.url}/${folderId}/directories`, { path });
  }

  deleteDirectory(folderId: string, path: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${folderId}/directories`, { params: { path } });
  }

  deleteFile(folderId: string, fileId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${folderId}/files/${fileId}`);
  }
}
