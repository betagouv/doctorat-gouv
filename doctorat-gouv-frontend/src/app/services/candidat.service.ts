import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ProfilResponse, ProfilUpdateRequest } from '../models/profil.model';
import { DemandeCandidat } from '../models/demande-candidat.model';
import { MessageEchange } from '../models/echange.model';
import { TableauDeBordCandidatItemBackend } from '../models/mise-en-relation.model';

@Injectable({ providedIn: 'root' })
export class CandidatService {

  private readonly apiUrl = `${environment.apiUrl}/candidat`;

  constructor(private http: HttpClient) {}

  getProfil(): Observable<ProfilResponse> {
    return this.http.get<ProfilResponse>(`${this.apiUrl}/profil`);
  }

  updateProfil(request: ProfilUpdateRequest): Observable<ProfilResponse> {
    return this.http.put<ProfilResponse>(`${this.apiUrl}/profil`, request);
  }

  addCompetence(competence: string): Observable<ProfilResponse> {
    return this.http.post<ProfilResponse>(`${this.apiUrl}/competences`, { competence });
  }

  removeCompetence(competence: string): Observable<ProfilResponse> {
    return this.http.delete<ProfilResponse>(`${this.apiUrl}/competences`, { body: { competence } });
  }

  changerMotDePasse(request: ChangementMotDePasseRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/mot-de-passe`, request);
  }

  getDemandesMiseEnRelation(): Observable<TableauDeBordCandidatItemBackend[]> {
    return this.http.get<TableauDeBordCandidatItemBackend[]>(`${this.apiUrl}/demande-mise-en-relation/mes-demandes`);
  }

  archiverDemande(demandeId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/demande-mise-en-relation/${demandeId}/archiver`, {});
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
