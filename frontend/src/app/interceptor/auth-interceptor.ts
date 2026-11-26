import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const isLogin = req.url.endsWith('/auth/login');
  const token = auth.getToken();

  const request = token && !isLogin
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(request).pipe(
    catchError((err: HttpErrorResponse) => {
      // Auto-logout si el token expiró o es inválido (el login fallido no cuenta)
      if (err.status === 401 && !isLogin) {
        auth.logout();
      }
      return throwError(() => err);
    })
  );
};
