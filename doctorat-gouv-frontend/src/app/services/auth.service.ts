import { Injectable, signal, computed } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap, of, map, catchError } from 'rxjs';
import { environment } from '../../environments/environment';
import { Utilisateur, ConnexionResponse, ConnexionRequest, InscriptionRequest } from '../models/utilisateur.model';

/**
 * Service d'authentification côté frontend.
 * Gère la connexion, l'inscription, la déconnexion et la persistance de la session
 * via JWT dans le localStorage. Expose un signal Angular pour l'état utilisateur courant.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly apiUrl = environment.apiUrl;
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'auth_user';

  private currentUserSignal = signal<Utilisateur | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUserSignal() !== null);

  constructor(private http: HttpClient) {
    this.loadFromStorage();
  }

  login(request: ConnexionRequest): Observable<ConnexionResponse> {
    const normalized = { ...request, email: this.normalizeEmail(request.email) };
    return this.http.post<ConnexionResponse>(`${this.apiUrl}/connexion`, normalized).pipe(
      tap(response => {
        this.setSession(response);
      })
    );
  }

  inscription(request: InscriptionRequest): Observable<ConnexionResponse> {
    const normalized = { ...request, email: this.normalizeEmail(request.email) };
    return this.http.post<ConnexionResponse>(`${this.apiUrl}/inscription`, normalized).pipe(
      tap(response => {
        this.setSession(response);
      })
    );
  }

  inscrireComplet(formData: FormData): Observable<ConnexionResponse> {
    return this.http.post<ConnexionResponse>(`${this.apiUrl}/inscription/complet`, formData).pipe(
      tap(response => {
        this.setSession(response);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSignal.set(null);
  }

  getCurrentUser(): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.apiUrl}/me`).pipe(
      tap(user => this.currentUserSignal.set(user))
    );
  }

  /**
   * Connexion par JWT reçu via PRC (retour OIDC : `?token=` dans l'URL).
   *
   * ⚠️ L'ordre est critique :
   * 1. On PURGE le localStorage AVANT l'appel — sinon `auth.interceptor` écrase
   *    notre header avec l'ancien token et `/api/me` échoue (401) ;
   * 2. on envoie le token reçu de façon EXPLICITE sur `/api/me` (vérification +
   *    récupération du profil) ;
   * 3. on stocke la session puis on PURGE l'URL (le token ne doit pas rester
   *    dans l'historique du navigateur ni l'onglet partageable).
   */
  loginWithToken(token: string): Observable<Utilisateur> {
    // 1. Purge AVANT l'appel (voir l'avertissement ci-dessus)
    this.logout();

    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    return this.http.get<Utilisateur>(`${this.apiUrl}/me`, { headers }).pipe(
      tap(user => {
        // 2 + 3. Session puis purge de l'URL
        this.setSession({ utilisateur: user, token, expiresIn: 0 });
        this.purgerUrl();
      })
    );
  }

  /**
   * Traite un éventuel `?token=` présent dans l'URL (retour de PRC).
   *
   * Branché en `APP_INITIALIZER` (`app.config.ts`) : l'application attend la fin
   * de ce traitement **avant** le routage, donc avant les gardes de route.
   * Sans ça, `authGuard` verrait un utilisateur non connecté et redirigerait
   * vers `/connexion` alors que le token est en cours de traitement.
   */
  traiterTokenDepuisUrl(): Observable<void> {
    const token = new URLSearchParams(window.location.search).get('token');

    if (!token) {
      return of(undefined);
    }

    return this.loginWithToken(token).pipe(
      map(() => undefined),
      catchError(() => {
        // Token expiré/invalidé par le backend : on repart d'une session propre
        this.logout();
        this.purgerUrl();
        return of(undefined);
      })
    );
  }

  /** Retire le token de l'URL sans recharger la page (il ne reste pas dans l'historique). */
  private purgerUrl(): void {
    window.history.replaceState({}, '', window.location.pathname);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUserId(): string | null {
    return this.currentUserSignal()?.id ?? null;
  }

  private setSession(response: ConnexionResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(response.utilisateur));
    this.currentUserSignal.set(response.utilisateur);
  }

  private normalizeEmail(email: string): string {
    return (email ?? '').trim().toLowerCase();
  }

  private loadFromStorage(): void {
    const token = localStorage.getItem(this.TOKEN_KEY);
    const userJson = localStorage.getItem(this.USER_KEY);
    if (token && userJson) {
      try {
        const user: Utilisateur = JSON.parse(userJson);
        this.currentUserSignal.set(user);
      } catch {
        this.logout();
      }
    }
  }
}
