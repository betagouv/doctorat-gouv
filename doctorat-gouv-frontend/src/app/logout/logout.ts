import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Composant /logout — point unique de déconnexion.
 *
 * <p>Phase 1 : stockage de l'id_token PRC côté front (localStorage).</p>
 *
 * <p>Si un id_token PRC est présent, on redirige le navigateur vers le
 * {@code end_session_endpoint} de PRC (déconnexion front-channel) avec
 * {@code id_token_hint}. Sinon, on purge la session locale et on revient à
 * l'accueil.</p>
 *
 * <p>TODO migration (voir {@code plan-developpement-complet.md} Lot F) :
 * stocker l'id_token côté backend (session HTTP ou table dédiée) et exposer
 * {@code /api/prc/logout}, pour ne plus le manipuler côté front — risques XSS,
 * incohérence multi-onglets, perte lors d'un clear storage.</p>
 */
@Component({
  selector: 'app-logout',
  standalone: true,
  template: '',
})
export class Logout implements OnInit {
  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    const prcToken = this.authService.getPrcIdToken();
    const endSessionUrl = this.authService.getEndSessionUrl();

    // Purge locale d'abord : quoi qu'il arrive ensuite, l'utilisateur est déconnecté de notre app.
    this.authService.logout();

    if (prcToken && endSessionUrl) {
      // Déconnexion PRC (front-channel) : PRC ferme sa session puis redirige vers
      // post_logout_redirect_uri.
      const state = crypto.randomUUID();
      const postLogout = encodeURIComponent(window.location.origin + '/');
      const url = `${endSessionUrl}?id_token_hint=${encodeURIComponent(prcToken)}&state=${state}&post_logout_redirect_uri=${postLogout}`;
      window.location.href = url;
    } else {
      this.router.navigate(['/']);
    }
  }
}
