import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DemandeDt, ProfilDtResponse, ProfilDtUpdateRequest, SujetDt } from '../models/profil.model';

@Injectable({ providedIn: 'root' })
export class DirecteurTheseService {

  private readonly apiUrl = `${environment.apiUrl}/directeur-these`;

  constructor(private http: HttpClient) {}

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

  getSujets(): Observable<SujetDt[]> {
    return this.http.get<SujetDt[]>(`${this.apiUrl}/sujets`);
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

  reinitialiserDemande(id: number): Observable<DemandeDt> {
    return this.http.post<DemandeDt>(`${this.apiUrl}/mises-en-relation/reinitialiser`, { id });
  }
}

export interface ChangementMotDePasseRequest {
  motDePasseActuel: string;
  nouveauMotDePasse: string;
  confirmationMotDePasse: string;
}
