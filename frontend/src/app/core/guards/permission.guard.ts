import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const permissionGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requiredPermissions = route.data['requiredPermissions'] as readonly string[] | undefined;

  if (!requiredPermissions?.length || auth.hasAnyPermission([...requiredPermissions])) {
    return true;
  }

  return router.parseUrl('/dashboard');
};
