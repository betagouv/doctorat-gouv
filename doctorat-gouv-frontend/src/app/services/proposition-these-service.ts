import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PropositionTheseDto, OffreEncadrementDto } from '../models/proposition-these-dto.model';import { PageResponse } from '../models/page-response.model';

@Injectable({
	providedIn: 'root',
})
export class PropositionTheseService {

	private baseUrl = `${environment.apiUrl}/propositions-these`;

	constructor(private http: HttpClient) { }

	/**
	 * Recherche des propositions de thèse avec filtres optionnels
	 * Exemple : service.search({ discipline: 'Informatique', localisation: 'Paris' })
	 */
	search(filters: Record<string, string>, page: number, size: number): Observable<PageResponse<PropositionTheseDto>> {
	  let params = new HttpParams()
	    .set('page', page)
	    .set('size', size);

	  Object.entries(filters).forEach(([key, value]) => {
	    if (value) {
	      params = params.set(key, value);
	    }
	  });

	  return this.http.get<PageResponse<PropositionTheseDto>>(this.baseUrl, { params });
	}
	
	/** ------------------------------------------------------------------
	 *  Récupérer une thèse par son ID
	 * ------------------------------------------------------------------ */
	getThesisById(id: number): Observable<PropositionTheseDto> {
	  const url = `${this.baseUrl}/proposition?id=${id}`;
	  return this.http.get<PropositionTheseDto>(url);
	}

	/**
	 * Autres offres d'encadrement du même directeur (fiche offre d'encadrement).
	 */
	getAutresOffres(id: number): Observable<PropositionTheseDto[]> {
	  const url = `${this.baseUrl}/proposition/autres-offres?id=${id}`;
	  return this.http.get<PropositionTheseDto[]>(url);
	}

	/**
	 * Fiche offre d'encadrement : offre + données publiques du chercheur.
	 */
	getOffreEncadrement(id: number): Observable<OffreEncadrementDto> {
	  const url = `${this.baseUrl}/proposition/encadrement?id=${id}`;
	  return this.http.get<OffreEncadrementDto>(url);
	}

}
