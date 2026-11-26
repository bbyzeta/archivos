import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Folder, FolderDirectory, FolderFile, FolderRequest } from '../models/folder.model';
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

  rename(id: string, name: string): Observable<Folder> {
    return this.http.put<Folder>(`${this.url}/${id}`, { name });
  }

  /** Asigna (o quita con null) el color de la carpeta. */
  setColor(id: string, color: string | null): Observable<Folder> {
    return this.http.patch<Folder>(`${this.url}/${id}/color`, { color });
  }

  /** Asigna (o quita con null) el color de una subcarpeta. */
  setDirectoryColor(folderId: string, path: string, color: string | null): Observable<FolderDirectory> {
    return this.http.put<FolderDirectory>(`${this.url}/${folderId}/directories/color`, { path, color });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
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

  listDirectories(folderId: string): Observable<FolderDirectory[]> {
    return this.http.get<FolderDirectory[]>(`${this.url}/${folderId}/directories`);
  }

  createDirectory(folderId: string, path: string): Observable<{ path: string }> {
    return this.http.post<{ path: string }>(`${this.url}/${folderId}/directories`, { path });
  }

  renameDirectory(folderId: string, path: string, name: string): Observable<{ path: string }> {
    return this.http.put<{ path: string }>(`${this.url}/${folderId}/directories`, { path, name });
  }

  /** `recursive` elimina también los archivos y subcarpetas que contenga. */
  deleteDirectory(folderId: string, path: string, recursive = false): Observable<void> {
    return this.http.delete<void>(`${this.url}/${folderId}/directories`, { params: { path, recursive } });
  }

  deleteFile(folderId: string, fileId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${folderId}/files/${fileId}`);
  }
}
