import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { authInterceptor } from './auth-interceptor';
import { AuthService } from '../services/auth.service';

const SESSION = { token: 'jwt-123', userId: 'u1', username: 'ana', fullName: 'Ana', role: 'USER' };

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let router: Router;

  function setup(withSession: boolean): void {
    sessionStorage.clear();
    if (withSession) sessionStorage.setItem('session', JSON.stringify(SESSION));

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  }

  afterEach(() => {
    controller.verify();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('agrega el Bearer a las peticiones cuando hay sesión', () => {
    setup(true);

    http.get('/api/admin/folders').subscribe();

    const req = controller.expectOne('/api/admin/folders');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-123');
    req.flush([]);
  });

  it('no agrega Authorization sin sesión', () => {
    setup(false);

    http.get('/api/public/abc').subscribe();

    const req = controller.expectOne('/api/public/abc');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('no envía el token al login', () => {
    setup(true);

    http.post('/api/auth/login', {}).subscribe();

    const req = controller.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('ante un 401 limpia la sesión y redirige a /login', () => {
    setup(true);
    const auth = TestBed.inject(AuthService);

    http.get('/api/admin/folders').subscribe({ error: () => undefined });
    controller.expectOne('/api/admin/folders').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(sessionStorage.getItem('session')).toBeNull();
    expect(auth.isLoggedIn()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('un 401 del propio login (credenciales incorrectas) no cierra sesión ni redirige', () => {
    setup(false);
    let status = 0;

    http.post('/api/auth/login', {}).subscribe({ error: (e) => (status = e.status) });
    controller.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(status).toBe(401);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('otros errores (400, 403, 409, 500) se propagan sin cerrar la sesión', () => {
    setup(true);

    for (const code of [400, 403, 409, 500]) {
      http.get(`/api/admin/x${code}`).subscribe({ error: () => undefined });
      controller.expectOne(`/api/admin/x${code}`).flush({}, { status: code, statusText: 'Error' });
    }

    expect(sessionStorage.getItem('session')).not.toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
