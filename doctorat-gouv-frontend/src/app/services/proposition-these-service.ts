import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PropositionTheseDto } from '../models/proposition-these-dto.model';
import { PageResponse } from '../models/page-response.model';

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

}
