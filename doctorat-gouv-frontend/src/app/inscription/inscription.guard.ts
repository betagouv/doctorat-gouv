import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { InscriptionStoreService } from '../services/inscription-store.service';

export const inscriptionGuard: CanActivateFn = () => {
  const store = inject(InscriptionStoreService);
  const router = inject(Router);
  if (store.coordonnees()) {
    return true;
  }
  return router.createUrlTree(['/inscription/coordonnees']);
};
