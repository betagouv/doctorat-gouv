import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';

import { PropositionTheseService } from '../services/proposition-these-service';
import { PropositionTheseDto } from '../models/proposition-these-dto.model';

import { DsfrHeaderModule } from '@edugouvfr/ngx-dsfr';
import { DsfrTagModule } from '@edugouvfr/ngx-dsfr';
import { DsfrFooterModule } from '@edugouvfr/ngx-dsfr';
import { DsfrButtonModule } from '@edugouvfr/ngx-dsfr';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [
    CommonModule,
	RouterModule,
    FormsModule,
    DsfrHeaderModule,
    DsfrTagModule,
    DsfrFooterModule,
    DsfrButtonModule
  ],
  templateUrl: './search.html',
  styleUrls: ['./search.scss']
})
export class Search {
	
  pageSize: number = 27;
  currentPage: number = 0;
  totalPages: number = 0;
	
  filters = {
    discipline: '',
    thematique: '',
    localisation: '',
    laboratoire: '',
    ecole: ''
  };

  keys = Object.keys(this.filters) as (keyof typeof this.filters)[];

  labels: Record<keyof typeof this.filters, string> = {
    discipline: 'Discipline',
    thematique: 'Thématique',
    localisation: 'Localisation géographique',
    laboratoire: 'Laboratoire de recherche',
    ecole: 'Établissement diplômant',
  };

  showMoreFilters = false;
  query: string = '';
  results: PropositionTheseDto[] = [];
  view: 'liste' | 'carte' = 'liste';

  constructor(
    private router: Router,
    private propositionService: PropositionTheseService
  ) {}

  onSearch(page: number = 0) {
    const activeFilters: Record<string, string> = { ...this.filters };
    if (this.query) {
      activeFilters['query'] = this.query;
    }

    this.propositionService.search(activeFilters, page, this.pageSize).subscribe({
      next: (data) => {
        this.results = data.content;
        this.currentPage = data.number;
        this.totalPages = data.totalPages;
		// 🔹 Scroll jusqu’au compteur de résultats
		document.getElementById('results-count')?.scrollIntoView({ behavior: 'smooth' });
      },
      error: (err) => {
        console.error('❌ Erreur lors de la recherche :', err);
      }
    });
  }
  
  goToNextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.onSearch(this.currentPage + 1);
    }
  }

  goToPreviousPage() {
    if (this.currentPage > 0) {
      this.onSearch(this.currentPage - 1);
    }
  }

  getPagesAround(): number[] {
    const start = Math.max(1, this.currentPage - 2);
    const end = Math.min(this.totalPages - 2, this.currentPage + 2);
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }


  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.onSearch(page);

      // 🔹 Scroll jusqu’au compteur de résultats
      const el = document.getElementById('results-count');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
        el.focus(); // accessibilité : annonce "X résultats de recherche"
      }
    }
  }


  onSearchForHeader(event: Event) {
    event.preventDefault();
    const input = (event.target as HTMLFormElement).querySelector<HTMLInputElement>('#search');
    const query = input?.value.trim();
    if (query) {
      this.router.navigate(['/recherche'], { queryParams: { q: query } });
    }
  }

  removeFilter(key: keyof typeof this.filters) {
    this.filters[key] = '';
    this.onSearch();
  }

  hasActiveFilters(): boolean {
    return this.keys.some(k => !!this.filters[k]);
  }

  toggleFilters() {
    this.showMoreFilters = !this.showMoreFilters;
  }
  
  getEntries(motsCles: Record<string, string> | null): [string, string][] {
		return motsCles ? Object.entries(motsCles) : [];
  }
  
  getResumeOrFallback(thesis: any, maxWords: number = 30): string {
    let text: string | null = null;

    if (thesis.resume) {
      text = `${thesis.resume}`;
    } else if (thesis.objectif) {
      text = `${thesis.objectif}`;
    } else if (thesis.context) {
      text = `${thesis.context}`;
    } else {
      return "Résumé non disponible";
    }

    // Limiter le nombre de mots
    const words = text.split(/\s+/);
    if (words.length > maxWords) {
      return words.slice(0, maxWords).join(" ") + " ...";
    }
    return text;
  }
  
  getImageForThesis(thesis: any): string {
    const mapping: { [key: string]: string } = {
      "Santé": "DS-1-Sante.jpg",
      "Culture, créativité, société": "DI-2-culture-creativite-societe.jpg",
      "Sécurité civile pour la société": "DS-3-Securité_civile.jpg",
      "Numérique, industrie, espace": "DI-4-numerique-industrie-espace.jpg",
      "Climat, énergie, mobilité": "DI-5-climat-energie-mobilite.jpg",
      "Alimentation, bioéconomie, ressources naturelles, agriculture et environnement": "DI-6-alimentation-bioeconomie.jpg"
    };

    // Vérifie que la liste existe et contient au moins un élément
    const domaine = thesis.domainesImpactListe?.[0];
    if (domaine && mapping[domaine]) {
      return `assets/images/${mapping[domaine]}`;
    }
    return "assets/images/default.jpg";
  }
  
  getFirstDomaine(thesis: { domainesImpactListe: string[] | null }): string | null {
    return thesis.domainesImpactListe && thesis.domainesImpactListe.length > 0
      ? thesis.domainesImpactListe[0]
      : null;
  }
  
  getFirstDomaineWithMaxLength(thesis: { domainesImpactListe: string[] | null }, maxLength: number = 10): string | null {
    if (thesis.domainesImpactListe && thesis.domainesImpactListe.length > 0) {
      const domaine = thesis.domainesImpactListe[0];
      return domaine.length > maxLength ? domaine.slice(0, maxLength) + "…" : domaine;
    }
    return null;
  }

}
