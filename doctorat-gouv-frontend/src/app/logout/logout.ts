import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';

/**
 * Composant /logout — point unique de déconnexion.
 *
 * Phase 1 : stockage de l'id_token PRC côté front (localStorage).
 * Page intermédiaire : l'utilisateur voit « Déconnexion réussie » puis est
 * redirigé vers PRC après 3 secondes (ou clique manuellement).
 *
 * TODO migration (Lot F) : stocker l'id_token côté backend + /api/prc/logout.
 */
@Component({
  selector: 'app-logout',
  standalone: true,
  template: `
    <main id="main-content" class="fr-container fr-my-8w">
      <div class="fr-grid-row fr-grid-row--center">
        <div class="fr-col-12 fr-col-md-8 fr-col-lg-6">
          <div class="fr-alert fr-alert--success fr-mb-4w" role="status">
            <h3 class="fr-alert__title">Déconnexion réussie</h3>
            <p>Vous êtes bien déconnecté de Doctorat.gouv.fr.</p>
          </div>

          <ng-container *ngIf="redirectionPrc">
            <p class="fr-text--sm fr-mb-3w">
              Redirection vers Projets Recherche Connect dans {{ compteRebours }} s…
            </p>
            <p>
              <a [href]="redirectionPrc" class="fr-btn fr-btn--primary">
                Continuer vers Projets Recherche Connect
              </a>
            </p>
            <p class="fr-text--sm fr-mt-2w">
              <a routerLink="/" class="fr-link">
                Retour à Doctorat.gouv.fr
              </a>
            </p>
          </ng-container>

          <ng-container *ngIf="!redirectionPrc">
            <p>
              <a routerLink="/" class="fr-btn fr-btn--primary">
                Retour à l'accueil
              </a>
            </p>
          </ng-container>
        </div>
      </div>
    </main>
  `,
  imports: [CommonModule, RouterLink],
})
export class Logout implements OnInit {
  redirectionPrc: string | null = null;
  compteRebours = 3;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    const prcToken = this.authService.getPrcIdToken();
    const endSessionUrl = this.authService.getEndSessionUrl();

    // Purge locale d'abord : quoi qu'il arrive ensuite, l'utilisateur est déconnecté de notre app.
    this.authService.logout();

    if (prcToken && endSessionUrl) {
      // PRC ne supporte pas post_logout_redirect_uri : on ne l'envoie pas.
      // Redirection automatique vers PRC après 3 secondes (l'utilisateur peut aussi cliquer).
      this.redirectionPrc = `${endSessionUrl}?id_token_hint=${encodeURIComponent(prcToken)}`;
      const timer = setInterval(() => {
        this.compteRebours--;
        if (this.compteRebours <= 0) {
          clearInterval(timer);
          window.location.href = this.redirectionPrc!;
        }
      }, 1000);
    } else {
      setTimeout(() => this.router.navigate(['/']), 1500);
    }
  }
}
