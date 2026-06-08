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

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { Subscription, Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

import { PropositionTheseService } from '../services/proposition-these-service';
import { PropositionTheseDto } from '../models/proposition-these-dto.model';
import { FilterService, AllFilterOptions } from '../services/filter.service';
import { SearchFiltersService } from '../services/search-filters-service';

import { DsfrHeaderModule } from '@edugouvfr/ngx-dsfr';
import { DsfrTagModule } from '@edugouvfr/ngx-dsfr';
import { DsfrFooterModule } from '@edugouvfr/ngx-dsfr';
import { DsfrButtonModule } from '@edugouvfr/ngx-dsfr';

import { TranslateModule } from '@ngx-translate/core';
import { TranslateService } from '@ngx-translate/core';
import { Nl2brPipe } from '../pipes/nl2br-pipe';

import { DynamicDatePipe } from '../pipes/dynamic-date-pipe';
import { ViewEncapsulation } from '@angular/core';

import { environment } from '../../environments/environment';

type MultiFilterKey =
  'discipline' |
  'localisation' |
  'laboratoire' |
  'ecole' |
  'defisSociete' |
  'annee';

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
    DsfrButtonModule,
	TranslateModule,
	DynamicDatePipe,
	Nl2brPipe
  ],
  templateUrl: './search.html',
  styleUrls: ['./search.scss'],
  encapsulation: ViewEncapsulation.None
})
export class Search implements OnInit, OnDestroy {
	
  // --- Filtres actifs pour typeOffer ---
  activeFilter: 'all' | 'thesis' | 'supervision' = 'all';

  /* ------------------- Pagination ------------------- */
  pageSize = 27;
  currentPage = 0;
  totalPages = 0;
  totalResults = 0;
  isInitialLoad = true;

  
  /* ------------------- Tri ------------------- */
  sortField: 'dateMiseEnLigne' | 'dateLimiteCandidature' | 'relevance' = 'dateMiseEnLigne';
  sortDirection: 'ASC' | 'DESC' = 'DESC';
  sortOpen = false;


  /* ------------------- Modèle de recherche ------------------- */
  query = '';

  discipline: string[] = [];
  localisation: string[] = [];
  laboratoire: string[] = [];
  ecole: string[] = [];
  defisSociete: string[] = [];
  ecoleDoctoraleNumero = '';
  etablissementRor = '';
  annee: string[] = [];

  /* ------------------- Options ------------------- */
  disciplineOpts: string[] = [];
  localisationOpts: string[] = [];
  laboratoireOpts: string[] = [];
  ecoleOpts: string[] = [];
  defisSocieteOpts: string[] = [];
  anneeOpts: string[] = [];

  /* ------------------- Dropdown states ------------------- */
  disciplineOpen = false;
  localisationOpen = false;
  laboratoireOpen = false;
  ecoleOpen = false;
  defisSocieteOpen = false;
  anneeOpen = false;

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


  /* ------------------- Reactive trigger ------------------- */
  private filterChanges$ = new Subject<void>();
  private filterSub!: Subscription;
  
  
  // --- Recherche IA Albert ---
  useAlbert = false;                  // case à cocher
  useSql = false;                     // recherche SQL en complément d'Albert
  albertQuery = '';                   // texte saisi (mode legacy)
  albertSearchQuery = '';             // texte saisi pour la recherche structurée
  albertResult: string | null = null; // résultat texte (mode legacy)
  isAlbertLoading = false;            // spinner
  isAlbertSearchActive = false;       // vrai quand on est en mode Albert enrichi

  // Données structurées retournées par /api/albert/propositions
  albertScores: Record<number, number> = {};
  albertMatchedTypes: Record<number, string> = {};
  albertSuggestedKeywords: string[] = [];
  aiMessage: string | null = null;

  // Interface pour la réponse Albert enrichie
  private albertResponseData: any = null;

  // --- Recherche vectorielle Scaleway ---
  scalewayQuery = '';
  isScalewayActive = false;
  isScalewayLoading = false;
  private scalewayResponseData: any = null;

  
  /* ------------------- Translations pour les filtres ------------------- */
  disciplineTranslations: Record<string, string> = {
    "Mathématiques et leurs interactions": "Mathematics and their interactions",
    "Physique": "Physics",
    "Sciences de la terre et de l'univers, espace": "Earth and universe sciences, space",
    "Chimie": "Chemistry",
    "Biologie, médecine et santé": "Biology, medicine and health",
    "Sciences humaines et humanités": "Human sciences and humanities",
    "Sciences de la société": "Social sciences",
    "Sciences pour l'ingénieur": "Engineering sciences",
    "Sciences et technologies de l'information et de la communication":
    "Information and communication sciences and technologies",
    "Sciences agronomiques et écologiques": "Agronomic and ecological sciences"
  };
  
  defisSocieteTranslations: Record<string, string> = {
    // Défis de société
    "Santé": "Health",
    "Culture, créativité, société": "Culture, creativity, society",
    "Sécurité civile pour la société": "Civil security for society",
    "Numérique, industrie, espace": "Digital, industry, space",
    "Climat, énergie, mobilité": "Climate, energy, mobility",
    "Alimentation, bioéconomie, ressources naturelles, agriculture et environnement":
      "Food, bioeconomy, natural resources, agriculture and environment",

    // ODD (ONU)
    "Pas de pauvreté": "No Poverty",
    "Faim \"zéro\"": "Zero Hunger",
    "Bonne santé et bien-être": "Good Health and Well-being",
    "Éducation de qualité": "Quality Education",
    "Égalité entre les sexes": "Gender Equality",
    "Eau propre et assainissement": "Clean Water and Sanitation",
    "Énergie propre et d'un coût abordable": "Affordable and Clean Energy",
    "Travail décent et croissance économique": "Decent Work and Economic Growth",
    "Industrie, innovation et infrastructure": "Industry, Innovation and Infrastructure",
    "Inégalités réduites": "Reduced Inequalities",
    "Villes et communautés durables": "Sustainable Cities and Communities",
    "Consommation et production responsables": "Responsible Consumption and Production",
    "Mesures relatives à la lutte contre les changements climatiques": "Climate Action",
    "Vie aquatique": "Life Below Water",
    "Vie terrestre": "Life on Land",
    "Paix, justice et institutions efficaces": "Peace, Justice and Strong Institutions",
    "Partenariats pour la réalisation des objectifs": "Partnerships for the Goals"
  };

  constructor(
	private route: ActivatedRoute,
    private router: Router,
    private propositionService: PropositionTheseService,
    private filterService: FilterService,
	private searchFiltersService: SearchFiltersService,
	public translate: TranslateService
  ) {}

  /* ------------------- Lifecycle ------------------- */
  ngOnInit(): void {
	  document.addEventListener('click', this.handleClickOutside.bind(this));
	  
	  // Restaurer les filtres sauvegardés
	  const saved = this.searchFiltersService.load();
	  
	  let urlHasEtablissementRor = false;
	  let urlHasEcoleDoctorale = false;

	  // Lecture synchrone des paramètres d'URL via le snapshot
	  // (évite le problème de timing asynchrone de queryParams.subscribe)
	  const urlParams = this.route.snapshot.queryParams;
	  if (urlParams['ecoledoctorale']) {

		  // Initialiser les filtres
		  this.discipline = saved?.discipline || [];
		  this.localisation = saved?.localisation || [];
		  this.laboratoire = saved?.laboratoire || [];
		  this.ecole = saved?.ecole || [];
		  this.defisSociete = saved?.defisSociete || [];
		  this.annee = saved?.annee || [];
		  this.etablissementRor = '';
		  this.query = '';

		  this.ecoleDoctoraleNumero = urlParams['ecoledoctorale'];
		  urlHasEcoleDoctorale = true;
	  }

	  if (urlParams['etablissementror']) {

		  // Initialiser les filtres
		  this.discipline = saved?.discipline || [];
		  this.localisation = saved?.localisation || [];
		  this.laboratoire = saved?.laboratoire || [];
		  this.ecole = saved?.ecole || [];
		  this.defisSociete = saved?.defisSociete || [];
		  this.annee = saved?.annee || [];
		  this.ecoleDoctoraleNumero = '';
		  this.query = '';

		  this.etablissementRor = urlParams['etablissementror'];
		  urlHasEtablissementRor = true;
	  }
	  
	  this.anneeOpts = this.generateYears();


	  this.loadFilterOptions();
	  
	  if (saved) {
	    this.query = saved.query || '';
	    this.discipline = Array.isArray(saved.discipline) ? saved.discipline : [];
	    this.localisation = Array.isArray(saved.localisation) ? saved.localisation : [];
	    this.laboratoire = Array.isArray(saved.laboratoire) ? saved.laboratoire : [];
	    this.ecole = Array.isArray(saved.ecole) ? saved.ecole : [];
	    this.defisSociete = Array.isArray(saved.defisSociete) ? saved.defisSociete : [];
		this.annee = Array.isArray(saved.annee) ? saved.annee : [];
		this.showMoreFilters = saved.showMoreFilters ?? false;
		
		// Ne PAS écraser la valeur venant de l’URL
		if (!urlHasEtablissementRor) {
		    this.etablissementRor = saved.etablissementRor || '';
		}
		
		// Ne PAS écraser la valeur venant de l’URL
		if (!urlHasEcoleDoctorale) {
			this.ecoleDoctoraleNumero = saved.ecoleDoctoraleNumero || '';
		}
		
		if (saved.typeProposition) {
		  this.activeFilter = saved.typeProposition;
		}
		
		if (saved.sortField) {
		  this.sortField = saved.sortField;
		}

 		if (saved.sortDirection) {
 		  this.sortDirection = saved.sortDirection;
 		}
 		
         this.currentPage = saved.page ?? 0;

		this.albertSearchQuery = saved.albertSearchQuery || '';
		this.useAlbert = saved.useAlbert || false;
		this.isAlbertSearchActive = saved.isAlbertSearchActive || false;
		this.albertScores = saved.albertScores || {};
		this.albertMatchedTypes = saved.albertMatchedTypes || {};
		this.albertSuggestedKeywords = saved.albertSuggestedKeywords || [];

		this.scalewayQuery = saved.scalewayQuery || '';
		this.isScalewayActive = saved.isScalewayActive || false;

		// Surcharge via paramètre d'URL pour debug (ex: ?useSql=false)
		if (urlParams['useSql'] !== undefined) {
		  this.useSql = urlParams['useSql'] === 'true';
		}

 	  }

	// Charger les résultats avec les filtres restaurés ou dès l'arrivée sur la page
	if (this.isScalewayActive && this.scalewayQuery.trim()) {
	  this.onScalewaySearch();
	} else if (this.isAlbertSearchActive && this.albertSearchQuery.trim()) {
	  this.onAlbertSearchPropositions();
	} else {
	  this.onSearch(this.currentPage);
	}
	this.isInitialLoad = false;
	
    this.filterSub = this.filterChanges$
      .pipe(debounceTime(300))
      .subscribe(() => this.onSearch(0));
  }
  
  ngAfterViewInit(): void {
    const saved = this.searchFiltersService.load();

    if (saved?.scrollPosition) {
      const target = saved.scrollPosition;

      const interval = setInterval(() => {
        const cards = document.querySelectorAll('.fr-card');
        if (cards.length > 0) {
          window.scrollTo({ top: target, behavior: 'auto' });
          clearInterval(interval);
        }
      }, 20);
    }
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
  
  filteredOptions(list: string[], search: string): string[] {
    if (!search) return list;
    return list.filter(opt =>
      opt.toLowerCase().includes(search.toLowerCase())
    );
  }

//  selectSingle(filterName: string, value: string): void {
//    (this as any)[filterName] = value;
//    this.onFilterChange();
//  }

resetFilter(filterName: MultiFilterKey) {
  this[filterName] = [] as any;
  this.onFilterChange();
}

  /* ------------------- Filters ------------------- */
  onFilterChange(): void {
    // Sauvegarder les filtres
		this.searchFiltersService.save({
		  query: this.query,
		  discipline: this.discipline,
		  localisation: this.localisation,
		  laboratoire: this.laboratoire,
		  ecole: this.ecole,
		  defisSociete: this.defisSociete,
		  ecoleDoctoraleNumero: this.ecoleDoctoraleNumero,
		  etablissementRor: this.etablissementRor,
		  typeProposition: this.activeFilter,
		  sortField: this.sortField,
		  sortDirection: this.sortDirection,
		  annee: this.annee,
		  showMoreFilters: this.showMoreFilters,
		  albertSearchQuery: this.albertSearchQuery,
		  useAlbert: this.useAlbert,
		  isAlbertSearchActive: this.isAlbertSearchActive,
		  albertScores: this.albertScores,
		  albertMatchedTypes: this.albertMatchedTypes,
		  albertSuggestedKeywords: this.albertSuggestedKeywords,
		  scalewayQuery: this.scalewayQuery,
		  isScalewayActive: this.isScalewayActive,
		  page: this.currentPage
		});

	// ⚠️ Ne pas déclencher filterChanges$ pendant le chargement initial
	if (!this.isInitialLoad) {
	  this.filterChanges$.next();
	}
	
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

	if (this.discipline.length > 0) active['discipline'] = this.discipline.join(';');
    if (this.localisation.length > 0) active['localisation'] = this.localisation.join(';');
    if (this.laboratoire.length > 0) active['laboratoire'] = this.laboratoire.join(';');
    if (this.ecole.length > 0) active['ecole'] = this.ecole.join(';');
    if (this.defisSociete.length > 0) active['defisSociete'] = this.defisSociete.join(';');
	if (this.annee.length > 0) active['annee'] = this.annee.join(';');
	if (this.ecoleDoctoraleNumero) {
	  active['ecoleDoctoraleNumero'] = this.ecoleDoctoraleNumero;
	}
	if (this.etablissementRor) {
	  active['etablissementRor'] = this.etablissementRor;
	}


    if (this.query?.trim()) active['query'] = this.query.trim();
	
	// AJOUT : filtre typeProposition
	if (this.activeFilter === 'thesis') {
	  active['typeProposition'] = 'proposition';
	} else if (this.activeFilter === 'supervision') {
	  active['typeProposition'] = 'offre';
	}
	
	active['sortField'] = this.sortField;
	active['sortDirection'] = this.sortDirection;

    return active;
  }

  /* ------------------- Search ------------------- */
  onSearch(page: number = 0): void {
    const activeFilters = this.buildActiveFilters();

    this.isAlbertSearchActive = false;
    this.isScalewayActive = false;
    this.propositionService.search(activeFilters, page, this.pageSize).subscribe({
      next: data => {
        this.results = data.content;
        this.currentPage = data.number;
        this.totalPages = data.totalPages;
        this.totalResults = data.totalElements;
		
		// Mettre à jour la page dans le storage
		this.searchFiltersService.save({
		  query: this.query,
		  discipline: this.discipline,
		  localisation: this.localisation,
		  laboratoire: this.laboratoire,
		  ecole: this.ecole,
		  defisSociete: this.defisSociete,
		  ecoleDoctoraleNumero: this.ecoleDoctoraleNumero,
		  etablissementRor: this.etablissementRor,
		  typeProposition: this.activeFilter,
		  sortField: this.sortField,
		  sortDirection: this.sortDirection,
		  annee: this.annee,
		  page: this.currentPage
		});
      },
      error: err => console.error('❌ Erreur lors de la recherche :', err)
    });
  }

  /** Active/désactive la recherche IA */
  toggleAlbert(): void {
    if (!this.useAlbert) {
      this.isAlbertSearchActive = false;
      this.albertSearchQuery = '';
      this.albertScores = {};
      this.albertMatchedTypes = {};
      this.albertSuggestedKeywords = [];
      this.results = [];
      this.totalResults = 0;
    }
    this.onFilterChange();
  }

  /**
   * Recherche sémantique via Albert, retourne les sujets sous forme de cards enrichies
   * avec scores, types d'intention et mots-clés suggérés.
   */
  onAlbertSearchPropositions(): void {
    const q = this.albertSearchQuery.trim();
    if (!q) return;

    this.isAlbertLoading = true;
    this.isAlbertSearchActive = true;
    this.isScalewayActive = false;
    this.albertSuggestedKeywords = [];
    this.albertScores = {};
    this.albertMatchedTypes = {};
    this.aiMessage = null;

    const url = `${environment.apiUrl}/albert/propositions?query=${encodeURIComponent(q)}&useSql=${this.useSql}`;

    fetch(url)
      .then(res => {
        if (!res.ok) throw new Error('Erreur HTTP ' + res.status);
        return res.json();
      })
      .then(data => {
        this.isAlbertLoading = false;

        // Stocker la réponse brute complète
        this.albertResponseData = data;

        // Pagination côté frontend sur les résultats Albert
        const allResults = data.results || [];
        this.totalResults = data.totalResults || allResults.length;
        this.totalPages = Math.max(1, Math.ceil(allResults.length / this.pageSize));
        this.currentPage = 0;
        this.results = allResults.slice(0, this.pageSize);

        // Scores et types d'intention
        if (data.scores) {
          this.albertScores = data.scores;
        }
        if (data.matchedTypes) {
          this.albertMatchedTypes = data.matchedTypes;
        }

        // Mots-clés suggérés
        this.albertSuggestedKeywords = data.suggestedKeywords || [];

        // Message d'indisponibilité Albert
        this.aiMessage = data.aiMessage || null;

        // Sauvegarder l'état Albert dans la session
        this.searchFiltersService.save({
          query: '',
          discipline: this.discipline,
          localisation: this.localisation,
          laboratoire: this.laboratoire,
          ecole: this.ecole,
          defisSociete: this.defisSociete,
          annee: this.annee,
          ecoleDoctoraleNumero: this.ecoleDoctoraleNumero,
          etablissementRor: this.etablissementRor,
          typeProposition: this.activeFilter,
          sortField: this.sortField,
          sortDirection: this.sortDirection,
          showMoreFilters: this.showMoreFilters,
          albertSearchQuery: this.albertSearchQuery,
          useAlbert: this.useAlbert,
          isAlbertSearchActive: this.isAlbertSearchActive,
          albertScores: this.albertScores,
          albertMatchedTypes: this.albertMatchedTypes,
          albertSuggestedKeywords: this.albertSuggestedKeywords,
          scrollPosition: 0
        });
      })
      .catch(err => {
        console.error(err);
        this.isAlbertLoading = false;
        this.isAlbertSearchActive = false;
        this.results = [];
        this.totalResults = 0;
        this.aiMessage = 'error';
      });
  }

  /* ------------------- Pagination ------------------- */
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      if (this.isAlbertSearchActive && this.albertResponseData) {
        const allResults = this.albertResponseData.results || [];
        this.currentPage = page;
        this.results = allResults.slice(page * this.pageSize, (page + 1) * this.pageSize);
      } else if (this.isScalewayActive && this.scalewayResponseData) {
        const allResults = this.scalewayResponseData.results || [];
        this.currentPage = page;
        this.results = allResults.slice(page * this.pageSize, (page + 1) * this.pageSize);
      } else {
        this.onSearch(page);
      }
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
  
  private urlParamMap: Record<string, string> = {
    ecoleDoctoraleNumero: 'ecoledoctorale',
    etablissementRor: 'etablissementror'
  };

  removeFilter(filterName: keyof Search): void {
    // 1) Supprimer la valeur du filtre
    (this as any)[filterName] = '';

    // 2) Trouver le nom du paramètre dans l’URL
    const urlParam = this.urlParamMap[filterName] || filterName;

    // 3) Mettre à jour l’URL
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { [urlParam]: null },
      queryParamsHandling: 'merge'
    });

    // 4) Relancer la recherche
    this.onFilterChange();
  }


  /* ------------------- Image helpers ------------------- */
  getEntries(motsCles: Record<string, string> | null): [string, string][] {
    return motsCles ? Object.entries(motsCles) : [];
  }

  getResumeOrFallback(thesis: any, maxWords = 30): string {
    let text = thesis?.resume || thesis?.objectif || thesis?.context || '';
    if (!text) return '';

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

  getFirstDomaineWithMaxLength(thesis: { domainesImpactListe: string[] | null }, maxLength = 60): string | null {
    const domaine = thesis.domainesImpactListe?.[0];
    if (!domaine) return null;

    // 🔥 Traduction FR → EN si nécessaire
    const label = this.getDomaineLabel(domaine);

    // 🔥 Coupe propre
    return label.length > maxLength ? label.slice(0, maxLength) + '…' : label;
  }

  
  getDomaineLabel(domaine: string): string {
    if (this.translate.currentLang === 'en') {
      return this.defisSocieteTranslations[domaine] || domaine;
    }
    return domaine;
  }
  
  getTranslatedValue(value: string): string {
    if (this.translate.currentLang !== 'en') {
      return value; // FR → on garde tel quel
    }

    // 🔥 Ordre de priorité : discipline → défis de société
    return (
      this.disciplineTranslations[value] ||
      this.defisSocieteTranslations[value] ||
      value // fallback FR
    );
  }
  
  getSpecialiteLabel(value: string): string {
    return this.getTranslatedValue(value);
  }
  
  getSpecialiteLabelWithMaxLength(value: string | null, maxLength = 60): string {
    if (!value) return '';

	const translated = value;
	// const translated = this.getTranslatedValue(value);
    return translated.length > maxLength ? translated.slice(0, maxLength) + '…' : translated;
  }


  closeAllDropdowns(): void {
    this.disciplineOpen = false;
    this.defisSocieteOpen = false;
    this.localisationOpen = false;
    this.laboratoireOpen = false;
    this.ecoleOpen = false;
	this.anneeOpen = false;
	this.sortOpen = false;

  }
  
  goToDetail(id: number): void {
    const selection = window.getSelection();
    if (selection && selection.toString().length > 0) {
      return; // l'utilisateur sélectionne du texte → ne pas naviguer
    }
	
	// Sauvegarder la position actuelle du scroll pour pouvoir y revenir après consultation du détail
	const pos = window.scrollY;
	const saved = this.searchFiltersService.load() || {};

	this.searchFiltersService.save({
	  ...saved,
	  scrollPosition: pos,
	  showMoreFilters: this.showMoreFilters
	});

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
  
  scrollToTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  getThesisTitle(thesis: any): string {
    const lang = this.translate.currentLang;

    if (lang === 'en') {
      // Si la version anglaise existe, on l'utilise
      if (thesis.theseTitreAnglais && thesis.theseTitreAnglais.trim() !== '') {
        return thesis.theseTitreAnglais;
      }
    }

    // Sinon fallback sur la version française
    return thesis.theseTitre;
  }
  
  getLocalizedResume(thesis: any, maxWords = 30): string {
    const lang = this.translate.currentLang;

    // Si la langue est EN et que resumeAnglais existe → on l'utilise
    if (lang === 'en' && thesis?.resumeAnglais && thesis.resumeAnglais.trim() !== '') {
      const words = thesis.resumeAnglais.split(/\s+/);
      return words.length > maxWords
        ? words.slice(0, maxWords).join(' ') + ' …'
        : thesis.resumeAnglais;
    }

    // Sinon fallback sur la version FR existante
    return this.getResumeOrFallback(thesis, maxWords);
  }
  
  getLocalizedKeywords(thesis: any): [string, string][] {
    const lang = this.translate.currentLang;

    // Si la langue est EN et que motsClesAnglais existe → on l'utilise
    if (
      lang === 'en' &&
      thesis?.motsClesAnglais &&
      Object.keys(thesis.motsClesAnglais).length > 0
    ) {
      return this.getEntries(thesis.motsClesAnglais);
    }

    // Sinon fallback sur la version FR
    return this.getEntries(thesis.motsCles);
  }

  getDisciplineLabel(opt: string): string {
    if (this.translate.currentLang === 'en') {
      return this.disciplineTranslations[opt] || opt;
    }
    return opt;
  }
  
  getDefisSocieteLabel(opt: string): string {
    if (this.translate.currentLang === 'en') {
      return this.defisSocieteTranslations[opt] || opt;
    }
    return opt;
  }
  
  setFilter(filter: 'all' | 'thesis' | 'supervision') {
    this.activeFilter = filter;
    this.onFilterChange();
  }
  
  setSortDirection(dir: 'ASC' | 'DESC') {
    this.sortDirection = dir;
    this.onFilterChange();
  }
  
  setSortField(field: 'dateMiseEnLigne' | 'dateLimiteCandidature' | 'relevance') {
    this.sortField = field as any;
    this.sortOpen = false;
    if (field === 'relevance') {
      this.sortDirection = 'DESC';
    }
    this.onFilterChange();
  }

  
  toggleSortDirection() {
    this.sortDirection = this.sortDirection === 'ASC' ? 'DESC' : 'ASC';
    this.onFilterChange();
  }
  
  generateYearsOld(): string[] {
    const current = new Date().getFullYear();
    const years: string[] = [];
    for (let y = current; y >= current - 1; y--) {
      years.push(String(y));
    }
    return years;
  }
  
  generateYears(): string[] {
    const current = new Date().getFullYear();

    // On veut N-1, N, N+1
    const years = [current - 1, current, current + 1];

    return years.map(y => String(y));
  }
  
  formatAcademicYear(year: string): string {
    const y = Number(year);
    return `${y}/${y + 1}`;
  }
  
  toggleMulti(filterName: MultiFilterKey, value: string) {
	const list = this[filterName] as string[];

	if (list.includes(value)) {
	  this[filterName] = list.filter(v => v !== value) as any;
	} else {
	  this[filterName] = [...list, value] as any;
	}

	this.onFilterChange();

  }
  
  getFilterLabelList(
    list: string[],
    type: 'discipline' | 'defisSociete' | 'localisation' | 'laboratoire' | 'ecole' | 'annee'
  ): string {

    if (!list || list.length === 0) return '';

    const label = this.getFilterTitle(type);

    // Fonction utilitaire : limite à 4 mots
    const truncateWords = (text: string, maxWords = 4): string => {
      const words = text.split(/\s+/);
      if (words.length <= maxWords) return text;
      return words.slice(0, maxWords).join(' ') + '…';
    };

    // 1 seul élément → badge avec libellé tronqué
    if (list.length === 1) {
      const first = this.getSingleLabel(list[0], type);
      const truncated = truncateWords(first, 4);
      return `
        ${label}
        <span class="filter-count-badge filter-single-badge">${truncated}</span>
      `;
    }

    // Plusieurs éléments → badge +X
    return `
      ${label}
      <span class="filter-count-badge">+${list.length}</span>
    `;
  }


  getFilterTitle(type: string): string {
    switch (type) {
      case 'discipline': return this.translate.instant('FILTERS.DISCIPLINE');
      case 'defisSociete': return this.translate.instant('FILTERS.DEFIS');
      case 'localisation': return this.translate.instant('FILTERS.LOCALISATION');
      case 'laboratoire': return this.translate.instant('FILTERS.LABO');
      case 'ecole': return this.translate.instant('FILTERS.ECOLE');
      case 'annee': return this.translate.instant('FILTERS.ANNEE');
      default: return '';
    }
  }

  private getSingleLabel(value: string, type: string): string {
    switch (type) {
      case 'discipline': return this.getDisciplineLabel(value);
      case 'defisSociete': return this.getDefisSocieteLabel(value);
      case 'annee': return this.formatAcademicYear(value);
      default: return value;
    }
  }

  removeValue(filterName: MultiFilterKey, value: string) {
    const list = this[filterName] as string[];
    this[filterName] = list.filter(v => v !== value) as any;
    this.onFilterChange();
  }
  
  toggleSelectAll(filterName: MultiFilterKey, options: string[]) {
    const current = this[filterName] as string[];

    // Si tout est déjà sélectionné → on vide
    if (current.length === options.length) {
      this[filterName] = [] as any;
    } 
    // Sinon → on sélectionne tout
    else {
      this[filterName] = [...options] as any;
    }

    this.onFilterChange();
  }
  
  resetAllFilters(): void {
    this.query = '';

    this.discipline = [];
    this.localisation = [];
    this.laboratoire = [];
    this.ecole = [];
    this.defisSociete = [];
    this.annee = [];

    this.ecoleDoctoraleNumero = '';
    this.etablissementRor = '';

    this.activeFilter = 'all';
    this.sortField = 'dateMiseEnLigne';
    this.sortDirection = 'DESC';

    this.showMoreFilters = false;

    this.currentPage = 0;

    this.albertSearchQuery = '';
    this.useAlbert = false;
    this.isAlbertSearchActive = false;
    this.isAlbertLoading = false;
    this.albertScores = {};
    this.albertMatchedTypes = {};
    this.albertSuggestedKeywords = [];

    this.scalewayQuery = '';
    this.isScalewayActive = false;
    this.isScalewayLoading = false;
    this.scalewayResponseData = null;

    // Fermer tous les dropdowns
    this.closeAllDropdowns();

    // Sauvegarder l’état vide
    this.searchFiltersService.save({
      query: '',
      discipline: [],
      localisation: [],
      laboratoire: [],
      ecole: [],
      defisSociete: [],
      annee: [],
      ecoleDoctoraleNumero: '',
      etablissementRor: '',
      typeProposition: 'all',
      sortField: 'dateMiseEnLigne',
      sortDirection: 'DESC',
      showMoreFilters: false,
      albertSearchQuery: '',
      useAlbert: false,
      isAlbertSearchActive: false,
      page: 0,
      scrollPosition: 0
    });

    // Relancer la recherche
    this.onSearch(0);
  }
  
  get activeFiltersCount(): number {
    let count = 0;
    count += this.discipline.length;
    count += this.localisation.length;
    count += this.laboratoire.length;
    count += this.ecole.length;
    count += this.defisSociete.length;
    count += this.annee.length;
    if (this.ecoleDoctoraleNumero) count++;
    if (this.etablissementRor) count++;
    if (this.query.trim()) count++;
    if (this.albertSearchQuery.trim()) count++;
    return count;
  }
  
  onAlbertSearch(): void {
		if (!this.albertQuery.trim()) {
			this.albertResult = "Veuillez saisir une question.";
			return;
		}

		this.isAlbertLoading = true;
		this.albertResult = null;

		fetch(`${environment.apiUrl}/albert/search?query=${encodeURIComponent(this.albertQuery)}`)
			.then(res => {
				if (!res.ok) {
					throw new Error("Erreur HTTP " + res.status);
				}
				return res.json();
			})
			.then(data => {
				this.isAlbertLoading = false;
				this.albertResult = data.answer || "Aucun résultat trouvé dans les sujets de thèse.";
			})
			.catch(err => {
				console.error(err);
				this.isAlbertLoading = false;
				this.albertResult = "Erreur lors de l'interrogation d'Albert.";
			});
	}

  /** Formate le score en pourcentage lisible */
  formatScore(score: number): string {
    return Math.round(score * 100) + '%';
  }

  /** Retourne la classe CSS pour la couleur du badge de score */
  getScoreColor(score: number): string {
    if (score >= 0.8) return 'score-high';
    if (score >= 0.6) return 'score-medium';
    return 'score-low';
  }

  /** Libellé lisible du type d'intention */
  getMatchedTypeLabel(type: string | null): string {
    if (!type) return '';
    const labels: Record<string, string> = {
      'mots_cles': 'Mots-clés',
      'resume': 'Résumé',
      'contexte': 'Contexte',
      'objectif': 'Objectif',
      'titre': 'Titre',
      'profil': 'Profil recherché',
      'general': 'Contenu général'
    };
    return labels[type] || type;
  }

  getMatchedTypeIcon(type: string | null): string {
    if (!type) return 'fr-icon-information-line';
    const icons: Record<string, string> = {
      'mots_cles': 'fr-icon-price-tag-line',
      'resume': 'fr-icon-draft-line',
      'contexte': 'fr-icon-folder-2-line',
      'objectif': 'fr-icon-target-line',
      'titre': 'fr-icon-article-line',
      'profil': 'fr-icon-user-line',
      'general': 'fr-icon-information-line'
    };
    return icons[type] || 'fr-icon-information-line';
  }

  /** Wrapper helpers that handle null thesis.id for strict template type checking */
  hasAlbertScore(thesis: any): boolean {
    return thesis.id != null && this.albertScores[thesis.id] !== undefined;
  }
  getAlbertScore(thesis: any): number {
    return thesis.id != null ? (this.albertScores[thesis.id] ?? 0) : 0;
  }
  hasAlbertMatchedType(thesis: any): boolean {
    return thesis.id != null && this.albertMatchedTypes[thesis.id] != null;
  }
  getAlbertMatchedType(thesis: any): string | null {
    return thesis.id != null ? this.albertMatchedTypes[thesis.id] : null;
  }

  /** Ajoute un mot-clé suggéré à la recherche */
  addSuggestedKeyword(keyword: string): void {
    const currentQuery = this.query.trim();
    this.query = currentQuery ? currentQuery + ' ' + keyword : keyword;
    this.onFilterChange();
  }

  /** Appliquer un mot-clé suggest comme requête de recherche Albert */
  searchByKeyword(keyword: string): void {
    this.query = keyword;
    if (this.useAlbert) {
      this.onFilterChange();
    } else {
      this.onSearch(0);
    }
  }

  /* ------------------- Recherche vectorielle Scaleway ------------------- */

  onScalewaySearch(): void {
    const q = this.scalewayQuery.trim();
    if (!q) return;

    this.isScalewayLoading = true;
    this.isScalewayActive = true;
    this.isAlbertSearchActive = false;
    this.scalewayResponseData = null;

    const url = `${environment.apiUrl}/scaleway/propositions?query=${encodeURIComponent(q)}&limit=100`;

    fetch(url)
      .then(res => {
        if (!res.ok) throw new Error('Erreur HTTP ' + res.status);
        return res.json();
      })
      .then(data => {
        this.isScalewayLoading = false;
        this.scalewayResponseData = data;

        const allResults = data.results || [];
        this.totalResults = data.totalResults || allResults.length;
        this.totalPages = Math.max(1, Math.ceil(allResults.length / this.pageSize));
        this.currentPage = 0;
        this.results = allResults.slice(0, this.pageSize);
      })
      .catch(err => {
        console.error(err);
        this.isScalewayLoading = false;
        this.isScalewayActive = false;
        this.results = [];
        this.totalResults = 0;
      });
  }

}
