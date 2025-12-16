/* src/app/services/filter.service.ts */
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { environment } from '../../environments/environment';

/* ------------------------------------------------------------------
   Interface décrivant la réponse du back‑end (Spring Boot)
   ------------------------------------------------------------------ */
export interface AllFilterOptions {
  discipline:   string[];
  localisation: string[];
  laboratoire:  string[];
  ecole:        string[];
  defisSociete: string[];
}

/* ------------------------------------------------------------------
   Service Angular – fourni dans la racine de l’application
   ------------------------------------------------------------------ */
@Injectable({ providedIn: 'root' })
export class FilterService {
  /** Préfixe commun à tous les endpoints du filtre */
  private readonly apiBase = `${environment.apiUrl}/filters`;

  constructor(private http: HttpClient) {}

  /* --------------------------------------------------------------
     Variante 1 – un seul appel qui renvoie toutes les listes
     -------------------------------------------------------------- */
  getAllOptions(): Observable<AllFilterOptions> {
    // → GET /api/filters/all
    return this.http.get<AllFilterOptions>(`${this.apiBase}/all`);
  }

}