import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ProfilDtResponse, ProfilDtUpdateRequest } from '../models/profil.model';

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
}

export interface ChangementMotDePasseRequest {
  motDePasseActuel: string;
  nouveauMotDePasse: string;
  confirmationMotDePasse: string;
}
