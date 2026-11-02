import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notify = inject(NotificationService);

  const token = authService.token();

  let authReq = req;
  if (token && !req.url.includes('/api/auth/login')) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // Les erreurs des endpoints d'authentification (identifiants invalides, blocage…) sont
  // gérées par l'écran de connexion : pas de déconnexion forcée ni de toast « session expirée ».
  const isAuthEndpoint = req.url.includes('/api/auth/');

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (isAuthEndpoint) {
        return throwError(() => error);
      }
      if (error.status === 401) {
        notify.error('Votre session a expiré. Veuillez vous reconnecter.', 'Session expirée');
        authService.logout();
      } else if (error.status === 403) {
        notify.warning('Vous n\'avez pas les permissions requises pour cette action.', 'Accès refusé');
      }
      return throwError(() => error);
    })
  );
};
