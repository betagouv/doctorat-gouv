import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ProfilResponse, ProfilUpdateRequest } from '../models/profil.model';

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
}

export interface ChangementMotDePasseRequest {
  motDePasseActuel: string;
  nouveauMotDePasse: string;
  confirmationMotDePasse: string;
}
