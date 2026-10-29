import { ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners, provideZoneChangeDetection, provideAppInitializer, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';
import { MatomoTrackingService } from './services/matomo-tracking-service';
import { authInterceptor } from './interceptors/auth.interceptor';
import { AuthService } from './services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes), provideClientHydration(withEventReplay()),
	provideHttpClient(withInterceptors([authInterceptor])),
	importProvidersFrom(TranslateModule.forRoot()),
	MatomoTrackingService,
    // PRC : traite un éventuel ?token= (retour OIDC) AVANT le routage, donc avant
    // les gardes — sinon authGuard redirigerait vers /connexion pendant le traitement.
    provideAppInitializer(() => inject(AuthService).traiterTokenDepuisUrl())
  ]
};
