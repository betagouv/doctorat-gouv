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
}
