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

import { Header } from '../header/header';

import { TranslateModule } from '@ngx-translate/core';
import { TranslateService } from '@ngx-translate/core';

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
    DsfrButtonModule,
	TranslateModule
  ],
  templateUrl: './search.html',
  styleUrls: ['./search.scss']
})
export class Search implements OnInit, OnDestroy {
	
  // --- Filtres actifs pour typeOffer ---
  activeFilter: 'all' | 'thesis' | 'supervision' = 'all';

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
  ecoleDoctoraleNumero = '';
  etablissementRor = '';

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


  /* ------------------- Reactive trigger ------------------- */
  private filterChanges$ = new Subject<void>();
  private filterSub!: Subscription;
  
  
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

	  // Lire les paramètres d’URL 
	  this.route.queryParams.subscribe(params => {
		  if (params['ecoledoctorale']) {

			  // Initialiser les filtres
			  this.discipline = ''; 
			  this.localisation = ''; 
			  this.laboratoire = ''; 
			  this.ecole = ''; 
			  this.defisSociete = ''; 
			  this.query = '';

			  this.ecoleDoctoraleNumero = params['ecoledoctorale'];
		  };
		  
		  if (params['etablissementror']) { 
			
			// Initialiser les filtres
			this.discipline = ''; 
			this.localisation = ''; 
			this.laboratoire = ''; 
			this.ecole = ''; 
			this.defisSociete = ''; 
			this.query = '';
			
			this.etablissementRor = params['etablissementror'];
		  }
	  });

	  this.loadFilterOptions();
	  
	  // 🔥 Restaurer les filtres sauvegardés
	  const saved = this.searchFiltersService.load();
	  if (saved) {
	    this.query = saved.query || '';
	    this.discipline = saved.discipline || '';
	    this.localisation = saved.localisation || '';
	    this.laboratoire = saved.laboratoire || '';
	    this.ecole = saved.ecole || '';
	    this.defisSociete = saved.defisSociete || '';
	    this.ecoleDoctoraleNumero = saved.ecoleDoctoraleNumero || '';
	    this.etablissementRor = saved.etablissementRor || '';
		
		if (saved.typeProposition) {
		  this.activeFilter = saved.typeProposition;
		}


	    // Relancer la recherche avec les filtres restaurés
	    this.onSearch(0);
	  }

	
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
	  typeProposition: this.activeFilter 
    });

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
	if (this.ecoleDoctoraleNumero) {
	  active['ecoleDoctoraleNumero'] = this.ecoleDoctoraleNumero;
	}
	if (this.etablissementRor) {
	  active['etablissementRor'] = this.etablissementRor;
	}


    if (this.query?.trim()) active['query'] = this.query.trim();
	
	// 🔥 AJOUT : filtre typeProposition
	if (this.activeFilter === 'thesis') {
	  active['typeProposition'] = 'proposition';
	} else if (this.activeFilter === 'supervision') {
	  active['typeProposition'] = 'offre';
	}

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
		
        // Après chargement des résultats, scroller vers le haut de la liste
        // document.getElementById('results-count')?.scrollIntoView({ behavior: 'smooth' });
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


}
