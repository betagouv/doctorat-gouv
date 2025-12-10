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

  onSearch() {
    const activeFilters: Record<string, string> = { ...this.filters };
    if (this.query) {
      activeFilters['query'] = this.query;
    }

    console.log('🔍 Filtres actifs envoyés au service :', activeFilters);

    this.propositionService.search(activeFilters).subscribe({
      next: (data) => {
        console.log('📦 Résultats reçus du backend :', data);
        this.results = data;
      },
      error: (err) => {
        console.error('❌ Erreur lors de la recherche :', err);
      }
    });
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
}
