import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DemandeDt, NotificationPreference, ProfilDtResponse, ProfilDtUpdateRequest, SujetDt } from '../models/profil.model';
import { MessageEchange } from '../models/echange.model';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class DirecteurTheseService {

  private readonly apiUrl = `${environment.apiUrl}/directeur-these`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  getProfil(): Observable<ProfilDtResponse> {
    return this.http.get<ProfilDtResponse>(`${this.apiUrl}/profil`);
  }

  updateProfil(request: ProfilDtUpdateRequest): Observable<ProfilDtResponse> {
    return this.http.put<ProfilDtResponse>(`${this.apiUrl}/profil`, request);
  }

  addCompetence(competence: string): Observable<ProfilDtResponse> {
    return this.http.post<ProfilDtResponse>(`${this.apiUrl}/competences`, { competence });
  }

  removeCompetence(competence: string): Observable<ProfilDtResponse> {
    return this.http.delete<ProfilDtResponse>(`${this.apiUrl}/competences`, { body: { competence } });
  }

  changerMotDePasse(request: ChangementMotDePasseRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/mot-de-passe`, request);
  }

  remplacerPhoto(fichier: File): Observable<ProfilDtResponse> {
    const formData = new FormData();
    formData.append('photo', fichier);
    return this.http.put<ProfilDtResponse>(`${this.apiUrl}/fichiers/photo`, formData);
  }

  supprimerPhoto(): Observable<ProfilDtResponse> {
    return this.http.delete<ProfilDtResponse>(`${this.apiUrl}/fichiers/photo`);
  }

  telechargerPhoto(): Observable<Blob> {
    const userId = this.authService.getUserId();
    return this.http.get(`${environment.apiUrl}/fichiers/utilisateurs/${userId}/photo`, { responseType: 'blob' });
  }

  getNotifications(): Observable<NotificationPreference> {
    return this.http.get<NotificationPreference>(`${this.apiUrl}/notifications`);
  }

  updateNotifications(preference: NotificationPreference): Observable<NotificationPreference> {
    return this.http.put<NotificationPreference>(`${this.apiUrl}/notifications`, preference);
  }

  getSujets(): Observable<SujetDt[]> {
    return this.http.get<SujetDt[]>(`${this.apiUrl}/sujets`);
  }

  getOffres(): Observable<SujetDt[]> {
    return this.http.get<SujetDt[]>(`${this.apiUrl}/offres`);
  }

  getDemandes(): Observable<DemandeDt[]> {
    return this.http.get<DemandeDt[]>(`${this.apiUrl}/mises-en-relation`);
  }

  getDemandeDetail(id: number): Observable<DemandeDt> {
    return this.http.post<DemandeDt>(`${this.apiUrl}/mises-en-relation/detail`, { id });
  }

  accepterDemande(id: number): Observable<DemandeDt> {
    return this.http.post<DemandeDt>(`${this.apiUrl}/mises-en-relation/accepter`, { id });
  }

  getMessages(demandeId: number): Observable<MessageEchange[]> {
    return this.http.post<MessageEchange[]>(`${this.apiUrl}/mises-en-relation/messages`, { demandeId });
  }

  envoyerMessage(demandeId: number, contenu: string): Observable<MessageEchange> {
    return this.http.post<MessageEchange>(`${this.apiUrl}/mises-en-relation/messages/envoyer`, { demandeId, contenu });
  }

  reinitialiserDemande(id: number): Observable<DemandeDt> {
    return this.http.post<DemandeDt>(`${this.apiUrl}/mises-en-relation/reinitialiser`, { id });
  }
}

export interface ChangementMotDePasseRequest {
  motDePasseActuel: string;
  nouveauMotDePasse: string;
  confirmationMotDePasse: string;
}
