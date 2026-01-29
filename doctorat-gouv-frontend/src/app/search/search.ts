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

/*****************************************************************************************
 *  SEARCH COMPONENT – version dropdown custom (mono‑sélection)
 *****************************************************************************************/

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subscription, Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

import { PropositionTheseService } from '../services/proposition-these-service';
import { PropositionTheseDto } from '../models/proposition-these-dto.model';
import { FilterService, AllFilterOptions } from '../services/filter.service';

import { DsfrHeaderModule } from '@edugouvfr/ngx-dsfr';
import { DsfrTagModule } from '@edugouvfr/ngx-dsfr';
import { DsfrFooterModule } from '@edugouvfr/ngx-dsfr';
import { DsfrButtonModule } from '@edugouvfr/ngx-dsfr';

import { Header } from '../header/header';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    Header,
    DsfrHeaderModule,
    DsfrTagModule,
    DsfrFooterModule,
    DsfrButtonModule
  ],
  templateUrl: './search.html',
  styleUrls: ['./search.scss']
})
export class Search implements OnInit, OnDestroy {

  /* ------------------- Pagination ------------------- */
  pageSize = 27;
  currentPage = 0;
  totalPages = 0;
  totalResults = 0;

  /* ------------------- Modèle de recherche ------------------- */
  query = '';

  discipline = '';
  localisation = '';
  laboratoire = '';
  ecole = '';
  defisSociete = '';

  /* ------------------- Options ------------------- */
  disciplineOpts: string[] = [];
  localisationOpts: string[] = [];
  laboratoireOpts: string[] = [];
  ecoleOpts: string[] = [];
  defisSocieteOpts: string[] = [];

  /* ------------------- Dropdown states ------------------- */
  disciplineOpen = false;
  localisationOpen = false;
  laboratoireOpen = false;
  ecoleOpen = false;
  defisSocieteOpen = false;

  /* ------------------- Search inside dropdown ------------------- */
  disciplineSearch = '';
  localisationSearch = '';
  laboratoireSearch = '';
  ecoleSearch = '';
  defisSocieteSearch = '';

  /* ------------------- UI ------------------- */
  showMoreFilters = false;
  results: PropositionTheseDto[] = [];
  view: 'liste' | 'carte' = 'liste';
  /*  dropdownPosition = {
    top: 0,
    left: 0,
    width: 0
  };*/


  /* ------------------- Reactive trigger ------------------- */
  private filterChanges$ = new Subject<void>();
  private filterSub!: Subscription;

  constructor(
    private router: Router,
    private propositionService: PropositionTheseService,
    private filterService: FilterService
  ) {}

  /* ------------------- Lifecycle ------------------- */
  ngOnInit(): void {
	document.addEventListener('click', this.handleClickOutside.bind(this));

    this.loadFilterOptions();
	
	// Charger les résultats dès l'arrivée sur la page 
	this.onSearch(0);

    this.filterSub = this.filterChanges$
      .pipe(debounceTime(300))
      .subscribe(() => this.onSearch(0));
  }

  ngOnDestroy(): void {
    if (this.filterSub) this.filterSub.unsubscribe();
	document.removeEventListener('click', this.handleClickOutside.bind(this));
  }

  /* ------------------- Dropdown logic ------------------- */
  handleClickOutside(event: MouseEvent): void {
    const target = event.target as HTMLElement;

    // Si on clique dans un dropdown → ne rien faire
    if (target.closest('.dropdown-filter')) return;

    // Sinon → fermer
    this.closeAllDropdowns();
  }

  toggleDropdown(panel: string): void {
    const isOpening = !(this as any)[panel];

    this.closeAllDropdowns();

    if (isOpening) {
      (this as any)[panel] = true;
    }
  }
  
/*  toggleDropdownOld(panel: string, event?: MouseEvent): void {
    const isOpening = !(this as any)[panel];

    this.closeAllDropdowns();

    if (isOpening && event) {
      const button = event.target as HTMLElement;
      const rect = button.getBoundingClientRect();

      this.dropdownPosition = {
        top: rect.bottom + window.scrollY,
        left: rect.left + window.scrollX,
        width: rect.width
      };

      (this as any)[panel] = true;
    }
  }*/



  filteredOptions(list: string[], search: string): string[] {
    if (!search) return list;
    return list.filter(opt =>
      opt.toLowerCase().includes(search.toLowerCase())
    );
  }

  selectSingle(filterName: string, value: string): void {
    (this as any)[filterName] = value;
    this.onFilterChange();
  }

  resetFilter(filterName: string): void {
    (this as any)[filterName] = '';
    this.onFilterChange();
  }

  /* ------------------- Filters ------------------- */
  onFilterChange(): void {
    this.filterChanges$.next();
  }

  private loadFilterOptions(): void {
    this.filterService.getAllOptions().subscribe({
      next: (data: AllFilterOptions) => {
        this.disciplineOpts = data.discipline;
        this.localisationOpts = data.localisation;
        this.laboratoireOpts = data.laboratoire;
        this.ecoleOpts = data.ecole;
        this.defisSocieteOpts = data.defisSociete;
      },
      error: err => console.error('Erreur lors du chargement des filtres', err)
    });
  }

  private buildActiveFilters(): Record<string, string> {
    const active: Record<string, string> = {};

    if (this.discipline) active['discipline'] = this.discipline;
    if (this.localisation) active['localisation'] = this.localisation;
    if (this.laboratoire) active['laboratoire'] = this.laboratoire;
    if (this.ecole) active['ecole'] = this.ecole;
    if (this.defisSociete) active['defisSociete'] = this.defisSociete;

    if (this.query?.trim()) active['query'] = this.query.trim();

    return active;
  }

  /* ------------------- Search ------------------- */
  onSearch(page: number = 0): void {
    const activeFilters = this.buildActiveFilters();

    this.propositionService.search(activeFilters, page, this.pageSize).subscribe({
      next: data => {
        this.results = data.content;
        this.currentPage = data.number;
        this.totalPages = data.totalPages;
        this.totalResults = data.totalElements;

        document.getElementById('results-count')
          ?.scrollIntoView({ behavior: 'smooth' });
      },
      error: err => console.error('❌ Erreur lors de la recherche :', err)
    });
  }

  /* ------------------- Pagination ------------------- */
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.onSearch(page);
      const el = document.getElementById('results-count');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
        el.focus();
      }
    }
  }

  getPagesAround(): number[] {
    const start = Math.max(1, this.currentPage - 2);
    const end = Math.min(this.totalPages - 2, this.currentPage + 2);
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  /* ------------------- Header search ------------------- */
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

  removeFilter(filterName: keyof Search): void {
    (this as any)[filterName] = '';
    this.onFilterChange();
  }

  /* ------------------- Image helpers ------------------- */
  getEntries(motsCles: Record<string, string> | null): [string, string][] {
    return motsCles ? Object.entries(motsCles) : [];
  }

  getResumeOrFallback(thesis: any, maxWords = 30): string {
    let text = thesis?.resume || thesis?.objectif || thesis?.context || '';
    if (!text) return 'Résumé non disponible';

    const words = text.split(/\s+/);
    return words.length > maxWords
      ? words.slice(0, maxWords).join(' ') + ' …'
      : text;
  }

  getImageForThesis(thesis: any): string {
    const oddMapping: Record<string, string> = {
      "Eau propre et assainissement": "ODD-6-eau-assainissement.jpg",
      "Consommation et production responsables": "ODD-12-consomation-responsable.jpg",
      "Mesures relatives à la lutte contre les changements climatiques": "ODD-13-changements-climatiques.jpg",
      "Bonne santé et bien-être": "ODD-3-sante.jpg",
      "Inégalités réduites": "ODD-10-inegalites-reduites.jpg",
      "Égalité entre les sexes": "ODD-5-egalite.jpg",
      "Industrie, innovation et infrastructure": "ODD-9-Industrie, innovation-et-infrastructure.jpg",
      "Éducation de qualité": "ODD-4-education.jpg",
      "Vie aquatique": "ODD-14-vie-aquatique.jpg",
      "Villes et communautés durables": "ODD-11-villes-durables.jpg",
      "Vie terrestre": "ODD-15-vie-terrestre.jpg",
      "Partenariats pour la réalisation des objectifs": "ODD-17-partenariats.jpg",
      "Énergie propre et d'un coût abordable": "ODD-7-energie-propre.jpg"
    };

    const impactMapping: Record<string, string> = {
      "Santé": "DS-1-Sante.jpg",
      "Culture, créativité, société": "DI-2-culture-creativite-societe.jpg",
      "Sécurité civile pour la société": "DS-3-Securite_civile.jpg",
      "Numérique, industrie, espace": "DI-4-numerique-industrie-espace.jpg",
      "Climat, énergie, mobilité": "DI-5-climat-energie-mobilite.jpg",
      "Alimentation, bioéconomie, ressources naturelles, agriculture et environnement":
        "DI-6-alimentation-bioeconomie.jpg"
    };

    const specialiteMapping: { key: string; file: string }[] = [
      { key: "Mathématique", file: "DS-1-mathematiques.jpg" },
      { key: "Physique", file: "DS-2-physique.jpg" },
      { key: "Sciences de la Terre et de l'Univers, Espace", file: "DS-3-terre-univers-espace.jpg" },
      { key: "Chimie", file: "DS-4-chimie.jpg" },
      { key: "éducation", file: "DS-6-Sciences-humaines-et-humanite.jpg" },
      { key: "sociale", file: "DS-6-Sciences humaines-et-humanite.jpg" },
      { key: "Agronomie", file: "DS-10-agronomique-ecologiques.jpg" },
      { key: "Ecologie", file: "DS-10-agronomique-ecologiques.jpg" },
      { key: "Biologie", file: "DS-5-biologie-medcine.jpg" }
    ];

    const odd = thesis.objectifsDeveloppementDurableListe?.[0];
    if (odd && oddMapping[odd]) {
      return `assets/images/odd/${oddMapping[odd]}`;
    }

    const impact = thesis.domainesImpactListe?.[0];
    if (impact && impactMapping[impact]) {
      return `assets/images/domaine_thematique/${impactMapping[impact]}`;
    }

    const specialite = thesis.specialite ?? "";
    for (const entry of specialiteMapping) {
      if (specialite.includes(entry.key)) {
        return `assets/images/domaine_scientifique/${entry.file}`;
      }
    }

    return "assets/images/default.jpg";
  }

  getFirstDomaine(thesis: { domainesImpactListe: string[] | null }): string | null {
    return thesis.domainesImpactListe?.[0] ?? null;
  }

  getFirstDomaineWithMaxLength(
    thesis: { domainesImpactListe: string[] | null },
    maxLength = 10
  ): string | null {
    const domaine = thesis.domainesImpactListe?.[0];
    return domaine
      ? domaine.length > maxLength
        ? domaine.slice(0, maxLength) + '…'
        : domaine
      : null;
  }
  
  closeAllDropdowns(): void {
    this.disciplineOpen = false;
    this.defisSocieteOpen = false;
    this.localisationOpen = false;
    this.laboratoireOpen = false;
    this.ecoleOpen = false;
  }
  
/*  goToDetail(id: number): void {
    this.router.navigate(['/proposition'], { queryParams: { id } });
  }*/
  
  goToDetail(id: number): void {
    const selection = window.getSelection();
    if (selection && selection.toString().length > 0) {
      return; // l'utilisateur sélectionne du texte → ne pas naviguer
    }

    this.router.navigate(['/proposition'], { queryParams: { id } });
  }

  limitWords(text: string | null, maxWords: number): string {
    if (!text) return '';
    const words = text.split(/\s+/);
    return words.length > maxWords
      ? words.slice(0, maxWords).join(' ') + '…'
      : text;
  }
  
  cleanVille(ville: string | null): string {
    if (!ville) return '';

    // Normalisation pour ignorer la casse
    const lower = ville.toLowerCase();

    // Si "cedex" est présent, on coupe avant
    if (lower.includes('cedex')) {
      return ville.substring(0, lower.indexOf('cedex')).trim();
    }

    return ville;
  }


}
