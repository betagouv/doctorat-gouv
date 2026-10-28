import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { NotificationPreference, ProfilResponse, ProfilUpdateRequest } from '../models/profil.model';
import { DemandeCandidat } from '../models/demande-candidat.model';
import { MessageEchange } from '../models/echange.model';
import { TableauDeBordCandidatItemBackend } from '../models/mise-en-relation.model';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class CandidatService {

  private readonly apiUrl = `${environment.apiUrl}/candidat`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  getProfil(): Observable<ProfilResponse> {
    return this.http.get<ProfilResponse>(`${this.apiUrl}/profil`);
  }

  updateProfil(request: ProfilUpdateRequest): Observable<ProfilResponse> {
    return this.http.put<ProfilResponse>(`${this.apiUrl}/profil`, request);
  }

  changerMotDePasse(request: ChangementMotDePasseRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/mot-de-passe`, request);
  }

  getNotifications(): Observable<NotificationPreference> {
    return this.http.get<NotificationPreference>(`${this.apiUrl}/notifications`);
  }

  updateNotifications(preference: NotificationPreference): Observable<NotificationPreference> {
    return this.http.put<NotificationPreference>(`${this.apiUrl}/notifications`, preference);
  }

  telechargerCv(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/fichiers/cv`, { responseType: 'blob' });
  }

  telechargerPiece(index: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/fichiers/pieces/${index}`, { responseType: 'blob' });
  }

  remplacerCv(fichier: File): Observable<ProfilResponse> {
    const formData = new FormData();
    formData.append('cv', fichier);
    return this.http.put<ProfilResponse>(`${this.apiUrl}/fichiers/cv`, formData);
  }

  ajouterPieces(fichiers: File[]): Observable<ProfilResponse> {
    const formData = new FormData();
    fichiers.forEach((f) => formData.append('pieces', f));
    return this.http.post<ProfilResponse>(`${this.apiUrl}/fichiers/pieces`, formData);
  }

  supprimerPiece(index: number): Observable<ProfilResponse> {
    return this.http.delete<ProfilResponse>(`${this.apiUrl}/fichiers/pieces/${index}`);
  }

  remplacerPhoto(fichier: File): Observable<ProfilResponse> {
    const formData = new FormData();
    formData.append('photo', fichier);
    return this.http.put<ProfilResponse>(`${this.apiUrl}/fichiers/photo`, formData);
  }

  supprimerPhoto(): Observable<ProfilResponse> {
    return this.http.delete<ProfilResponse>(`${this.apiUrl}/fichiers/photo`);
  }

  telechargerPhoto(): Observable<Blob> {
    const userId = this.authService.getUserId();
    return this.http.get(`${environment.apiUrl}/fichiers/utilisateurs/${userId}/photo`, { responseType: 'blob' });
  }

  getDemandesMiseEnRelation(): Observable<TableauDeBordCandidatItemBackend[]> {
    return this.http.get<TableauDeBordCandidatItemBackend[]>(`${this.apiUrl}/demande-mise-en-relation/mes-demandes`);
  }

  archiverDemande(demandeId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/demande-mise-en-relation/${demandeId}/archiver`, {});
  }

  desarchiverDemande(demandeId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/demande-mise-en-relation/${demandeId}/desarchiver`, {});
  }

  getDemandeDetail(demandeId: number): Observable<DemandeCandidat> {
    return this.http.post<DemandeCandidat>(`${this.apiUrl}/demande-mise-en-relation/detail`, { demandeId });
  }

  getMessages(demandeId: number): Observable<MessageEchange[]> {
    return this.http.post<MessageEchange[]>(`${this.apiUrl}/demande-mise-en-relation/messages`, { demandeId });
  }

  envoyerMessage(demandeId: number, contenu: string): Observable<MessageEchange> {
    return this.http.post<MessageEchange>(`${this.apiUrl}/demande-mise-en-relation/messages/envoyer`, { demandeId, contenu });
  }

  seDesister(demandeId: number, motif: string): Observable<DemandeCandidat> {
    return this.http.post<DemandeCandidat>(`${this.apiUrl}/demande-mise-en-relation/desister`, { demandeId, motif });
  }

  annulerDesistement(demandeId: number): Observable<DemandeCandidat> {
    return this.http.post<DemandeCandidat>(`${this.apiUrl}/demande-mise-en-relation/annuler-desistement`, { demandeId });
  }
}

export interface ChangementMotDePasseRequest {
  motDePasseActuel: string;
  nouveauMotDePasse: string;
  confirmationMotDePasse: string;
}
