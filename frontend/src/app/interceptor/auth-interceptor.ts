import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.includes('/auth/login')) {
    return next(req);
  }

  const usuarioSession = sessionStorage.getItem('usuario');
  
  if (usuarioSession) {
    try {
      const usuario = JSON.parse(usuarioSession);
      const token = usuario?.token;

      if (token) {
        const clonedReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
        return next(clonedReq);
      }
    } catch (e) {
      console.error('Error parseando el usuario de sessionStorage:', e);
    }
  }

  return next(req);
};