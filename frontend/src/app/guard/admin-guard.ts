import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Solo administradores; el resto vuelve a /home. Se usa junto con authGuard. */
export const adminGuard: CanActivateFn = () => {
  return inject(AuthService).isAdmin() ? true : inject(Router).createUrlTree(['/home']);
};
