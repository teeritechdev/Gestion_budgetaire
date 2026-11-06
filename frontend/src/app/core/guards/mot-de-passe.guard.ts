import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Mot de passe temporaire : seule la page de profil (onglet Sécurité) reste accessible. */
export const motDePasseGuard: CanActivateChildFn = (_route, state) => {
  const auth = inject(AuthService);
  if (!auth.currentUser()?.doitChangerMotDePasse || state.url.startsWith('/profil')) {
    return true;
  }
  return inject(Router).createUrlTree(['/profil'], { queryParams: { onglet: 'securite' } });
};
