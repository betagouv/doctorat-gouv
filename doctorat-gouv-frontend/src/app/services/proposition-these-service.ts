import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PropositionTheseDto } from '../models/proposition-these-dto.model';


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
	search(filters: Record<string, string>): Observable<PropositionTheseDto[]> {
		let params = new HttpParams();
		Object.entries(filters).forEach(([key, value]) => {
			if (value) {
				params = params.set(key, value);
			}
		});

		return this.http.get<PropositionTheseDto[]>(this.baseUrl, { params });
	}
}
