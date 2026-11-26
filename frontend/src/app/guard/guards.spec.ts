import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { adminGuard } from './admin-guard';
import { authGuard } from './auth-guard';

function run(guard: typeof authGuard): boolean | UrlTree {
  return TestBed.runInInjectionContext(() =>
    guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)) as boolean | UrlTree;
}

function login(role: string): void {
  sessionStorage.setItem('session', JSON.stringify({ token: 't', userId: 'u', username: 'x', fullName: 'X', role }));
}

describe('guards', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  afterEach(() => sessionStorage.clear());

  it('authGuard deja pasar con sesión', () => {
    login('USER');
    expect(run(authGuard)).toBe(true);
  });

  it('authGuard redirige a /login sin sesión', () => {
    const result = run(authGuard) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/login');
  });

  it('adminGuard deja pasar a un administrador', () => {
    login('ADMIN');
    expect(run(adminGuard)).toBe(true);
  });

  it('adminGuard redirige a /home a un usuario normal', () => {
    login('USER');
    const result = run(adminGuard) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/home');
  });

  it('adminGuard redirige a /home sin sesión (authGuard actúa antes en la ruta)', () => {
    const result = run(adminGuard) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/home');
  });
});
