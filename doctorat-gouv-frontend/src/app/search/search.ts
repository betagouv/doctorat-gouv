/*****************************************************************************************
 *  SEARCH COMPONENT – version « filtre par filtre » (standalone)
 *
 *  Ce composant :
 *   • charge les options de chaque filtre via FilterService
 *   • expose un champ model distinct pour chaque filtre
 *   • construit l’objet de recherche à la volée
 *
 *  Remarque : Il faut disposer d’un endpoint API qui renvoie
 *  toutes les listes d’options (ex. GET /api/filters/all) ou, à défaut,
 *  plusieurs endpoints séparés.
 *  
 *****************************************************************************************/

import { Component, OnInit, OnDestroy  } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subscription, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { PropositionTheseService } from '../services/proposition-these-service';
import { PropositionTheseDto } from '../models/proposition-these-dto.model';
import { PageResponse } from '../models/page-response.model';

import { DsfrHeaderModule } from '@edugouvfr/ngx-dsfr';
import { DsfrTagModule } from '@edugouvfr/ngx-dsfr';
import { DsfrFooterModule } from '@edugouvfr/ngx-dsfr';
import { DsfrButtonModule } from '@edugouvfr/ngx-dsfr';

/* ---------- SERVICE QUI FOURNIT LES OPTIONS DE FILTRE ---------- */
import { FilterService, AllFilterOptions } from '../services/filter.service';
/* -------------------------------------------------------------- */

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
export class Search implements OnInit {
  /* ------------------- Pagination ------------------- */
  pageSize = 27;
  currentPage = 0;
  totalPages = 0;
  totalResults = 0;               // nombre total de résultats (global)

  /* ------------------- Modèle de recherche ------------------- */
  query = '';

  discipline = '';
  localisation = '';
  laboratoire = '';
  ecole = '';
  defisSociete = '';

  /* ------------------- Options affichées dans chaque <select> ------------------- */
  disciplineOpts: string[] = [];
  localisationOpts: string[] = [];
  laboratoireOpts: string[] = [];
  ecoleOpts: string[] = [];
  defisSocieteOpts: string[] = [];

  /* ------------------- UI ------------------- */
  showMoreFilters = false;        // affichage de la deuxième rangée de filtres
  results: PropositionTheseDto[] = [];
  view: 'liste' | 'carte' = 'liste';
  
  /* ------------------- Reactive trigger ------------------- */
  private filterChanges$ = new Subject<void>();
  private filterSub!: Subscription;

  /* ------------------- Injection de dépendances ------------------- */
  constructor(
    private router: Router,
    private propositionService: PropositionTheseService,
    private filterService: FilterService          // ← nouveau service
  ) {}

  /* ------------------- Cycle de vie ------------------- */
  ngOnInit(): void {
	this.loadFilterOptions();

	// 1️ - Souscrire au Subject avec debounce
	this.filterSub = this.filterChanges$
	  .pipe(
	    debounceTime(300),          // attendre 300 ms d’inactivité
	    // distinctUntilChanged()      // ne pas relancer si la valeur n’a pas changé
	  )
	  .subscribe(() => {
	    // Chaque fois que le debounce se déclenche, on lance la recherche
	    this.onSearch(0);          // on repart toujours à la première page
	  });
  }
  
  ngOnDestroy(): void {
    // Nettoyer l’abonnement pour éviter les fuites mémoire
    if (this.filterSub) {
      this.filterSub.unsubscribe();
    }
  }
  
  /* -----------------------------------------------------------------
     Méthode appelée à chaque changement de champ
     ----------------------------------------------------------------- */
  onFilterChange(): void {
    // On pousse simplement un signal dans le Subject.
    // Le debounce gérera le timing réel.
    this.filterChanges$.next();
  }

  /** Charge toutes les listes d’options depuis le back‑end */
  private loadFilterOptions(): void {
    this.filterService.getAllOptions().subscribe({
      next: (data: AllFilterOptions) => {
        this.disciplineOpts   = data.discipline;
        this.localisationOpts = data.localisation;
        this.laboratoireOpts  = data.laboratoire;
        this.ecoleOpts        = data.ecole;
		this.defisSocieteOpts = data.defisSociete;
      },
      error: err => {
        console.error('Erreur lors du chargement des filtres', err);
        // Vous pouvez afficher un toast ou un message d’erreur à l’utilisateur ici.
      }
    });
  }

  /* ------------------- Recherche ------------------- */
  /** Construit l’objet de filtres à partir des champs remplis */
  private buildActiveFilters(): Record<string, string> {
    const active: Record<string, string> = {};

    // Chaque champ n’est ajouté que s’il possède une valeur non vide
    if (this.discipline)   active['discipline']   = this.discipline;
    if (this.localisation) active['localisation'] = this.localisation;
    if (this.laboratoire)  active['laboratoire']  = this.laboratoire;
    if (this.ecole)        active['ecole']        = this.ecole;
	if (this.defisSociete) active['defisSociete'] = this.defisSociete;

    // Le champ de recherche libre (titre / mots‑clés)
    if (this.query?.trim()) {
      active['query'] = this.query.trim();
    }

    return active;
  }

  /** Lance la recherche (page = 0 par défaut) */
  onSearch(page: number = 0): void {
    const activeFilters = this.buildActiveFilters();

    this.propositionService.search(activeFilters, page, this.pageSize).subscribe({
      next: data => {
        this.results      = data.content;
        this.currentPage  = data.number;
        this.totalPages   = data.totalPages;
        this.totalResults = data.totalElements;   // nombre total de résultats (global)

        // Scroll jusqu’au compteur de résultats
        document.getElementById('results-count')
                ?.scrollIntoView({ behavior: 'smooth' });
      },
      error: err => console.error('❌ Erreur lors de la recherche :', err)
    });
  }

  /* ------------------- Pagination (next / previous / specific) ------------------- */
  goToNextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.onSearch(this.currentPage + 1);
    }
  }

  goToPreviousPage(): void {
    if (this.currentPage > 0) {
      this.onSearch(this.currentPage - 1);
    }
  }

  /** Retourne un tableau d’indice de pages autour de la page courante (pour l’affichage) */
  getPagesAround(): number[] {
    const start = Math.max(1, this.currentPage - 2);
    const end   = Math.min(this.totalPages - 2, this.currentPage + 2);
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.onSearch(page);

      // Scroll jusqu’au compteur de résultats + focus (accessibilité)
      const el = document.getElementById('results-count');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
        el.focus();
      }
    }
  }

  /* ------------------- Recherche depuis l’en‑tête du site ------------------- */
  onSearchForHeader(event: Event): void {
    event.preventDefault();
    const input = (event.target as HTMLFormElement)
                    .querySelector<HTMLInputElement>('#search');
    const query = input?.value.trim();

    if (query) {
      this.router.navigate(['/recherche'], { queryParams: { q: query } });
    }
  }

  /* ------------------- UI helpers ------------------- */
  toggleFilters(): void {
    this.showMoreFilters = !this.showMoreFilters;
  }

  /** Transforme un dictionnaire de mots clés en tableau d’entrée */
  getEntries(motsCles: Record<string, string> | null): [string, string][] {
    return motsCles ? Object.entries(motsCles) : [];
  }

  /**
   * Retourne le résumé (ou un fallback) tronqué à `maxWords` mots.
   * Si aucune des propriétés attendues n’est renseignée, on renvoie
   * « Résumé non disponible ».
   */
  getResumeOrFallback(thesis: any, maxWords = 30): string {
    // 1️ - On commence avec une chaîne vide – jamais null.
    let text = '';

    // 2️ - On remplit `text` dès qu’on trouve une valeur.
    if (thesis?.resume) {
      text = thesis.resume;
    } else if (thesis?.objectif) {
      text = thesis.objectif;
    } else if (thesis?.context) {
      text = thesis.context;
    } else {
      // Aucun champ trouvé → on sort immédiatement.
      return 'Résumé non disponible';
    }

    // 3️ - On découpe la chaîne en mots et on limite le nombre de mots.
    const words = text.split(/\s+/);
    if (words.length > maxWords) {
      return words.slice(0, maxWords).join(' ') + ' …';
    }

    // 4️ - Retour du texte complet
    return text;
  }

  /** Retourne le chemin d’image correspondant au domaine d’impact */
  getImageForThesis(thesis: any): string {
    const mapping: { [key: string]: string } = {
      'Santé':                                   'DS-1-Sante.jpg',
      'Culture, créativité, société':            'DI-2-culture-creativite-societe.jpg',
      'Sécurité civile pour la société':        'DS-3-Securité_civile.jpg',
      'Numérique, industrie, espace':            'DI-4-numerique-industrie-espace.jpg',
      'Climat, énergie, mobilité':              'DI-5-climat-energie-mobilite.jpg',
      'Alimentation, bioéconomie, ressources naturelles, agriculture et environnement':
                                                'DI-6-alimentation-bioeconomie.jpg'
    };

    const domaine = thesis.domainesImpactListe?.[0];
    return domaine && mapping[domaine]
      ? `assets/images/${mapping[domaine]}`
      : 'assets/images/default.jpg';
  }

  getFirstDomaine(thesis: { domainesImpactListe: string[] | null }): string | null {
    return thesis.domainesImpactListe && thesis.domainesImpactListe.length > 0
      ? thesis.domainesImpactListe[0]
      : null;
  }

  getFirstDomaineWithMaxLength(
    thesis: { domainesImpactListe: string[] | null },
    maxLength = 10
  ): string | null {
    if (thesis.domainesImpactListe && thesis.domainesImpactListe.length > 0) {
      const domaine = thesis.domainesImpactListe[0];
      return domaine.length > maxLength ? domaine.slice(0, maxLength) + '…' : domaine;
    }
    return null;
  }
  
  /** Réinitialise le filtre indiqué et relance la recherche */
  removeFilter(filterName: keyof Search): void {
    // 1️ - Remettre la propriété à la valeur vide
    (this as any)[filterName] = '';

    // 2️ - Notifier le système de changement de filtre
    this.onFilterChange();   // <-- déclenche le Subject → debounce → onSearch()
  }
}