import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { Title, DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PropositionTheseService } from '../services/proposition-these-service';
import { ContactContextService } from '../services/contact-context-service';
import { PropositionTheseDto, OffreEncadrementDto, DirecteurOffreDto } from '../models/proposition-these-dto.model';
import { Nl2brPipe } from '../pipes/nl2br-pipe';

// Page volontairement sans i18n (textes en français en dur).
const MOIS = [
  'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
  'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'
];

@Component({
  selector: 'app-offre-encadrement',
  standalone: true,
  imports: [CommonModule, RouterModule, Nl2brPipe],
  templateUrl: './offre-encadrement.html',
  styleUrl: './offre-encadrement.scss',
})
export class OffreEncadrement implements OnInit {

  offreId: number = NaN;
  offre: PropositionTheseDto | null = null;
  directeur: DirecteurOffreDto | null = null;
  autresOffres: PropositionTheseDto[] = [];
  isLoading = true;
  errorMessage: string | null = null;
  encadrementEtendu = false;
  photoErreur = false;
  mapLat: number | null = null;
  mapLon: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private propositionTheseService: PropositionTheseService,
    private contactContextService: ContactContextService,
    private titleService: Title,
    private sanitizer: DomSanitizer,
    private http: HttpClient,
  ) {}

  ngOnInit(): void {
    // Souscription (et non snapshot) pour suivre la navigation entre offres
    // depuis le bloc "Autres offres d'encadrement".
    this.route.queryParamMap.subscribe(params => {
      const idParam = params.get('id');
      this.offreId = idParam ? Number(idParam) : NaN;
      this.encadrementEtendu = false;
      this.photoErreur = false;
      this.chargerOffre();
    });
  }

  chargerOffre(): void {
    if (!this.offreId || isNaN(this.offreId)) {
      this.isLoading = false;
      this.errorMessage = "Cette offre n'est pas disponible.";
      return;
    }
    this.isLoading = true;
    this.errorMessage = null;
    this.propositionTheseService.getOffreEncadrement(this.offreId).subscribe({
      next: (data: OffreEncadrementDto) => {
        this.offre = data?.offre ?? null;
        this.directeur = data?.directeur ?? null;
        this.isLoading = false;
        if (!this.offre) {
          this.errorMessage = "Cette offre n'est pas disponible.";
          return;
        }
        const titre = this.offre.theseTitre || 'Offre d\u2019encadrement';
        this.titleService.setTitle(`${titre} — Offre d'encadrement — Doctorat.gouv.fr`);
        this.chargerAutresOffres();
        this.geocoderLieu();
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = "Cette offre n'est pas disponible.";
      }
    });
  }

  chargerAutresOffres(): void {
    if (!this.offreId || isNaN(this.offreId)) {
      return;
    }
    this.propositionTheseService.getAutresOffres(this.offreId).subscribe({
      next: (data) => {
        this.autresOffres = (data ?? []).filter(o => o.id !== this.offreId);
      },
      error: () => {
        this.autresOffres = [];
      }
    });
  }

  get nomComplet(): string {
    const prenom = (this.directeur?.prenom ?? this.offre?.directionThesePrenom ?? '').trim();
    const nom = (this.directeur?.nom ?? this.offre?.directionTheseNom ?? '').trim();
    return `${prenom} ${nom}`.trim() || 'Chercheur';
  }

  get civiliteNom(): string {
    const civilite = (this.directeur?.civilite ?? '').trim();
    return civilite ? `${civilite} ${this.nomComplet}`.trim() : this.nomComplet;
  }

  get initiales(): string {
    const prenom = (this.directeur?.prenom ?? this.offre?.directionThesePrenom ?? '').trim();
    const nom = (this.directeur?.nom ?? this.offre?.directionTheseNom ?? '').trim();
    return `${prenom.charAt(0)}${nom.charAt(0)}`.toUpperCase() || '?';
  }

  get photoUrl(): string | null {
    const url = (this.directeur?.photoUrl ?? '').trim();
    return url ? url : null;
  }

  get orcid(): string | null {
    const orcid = (this.directeur?.orcid ?? this.offre?.directionTheseOrcid ?? '').trim();
    return orcid ? orcid : null;
  }

  get orcidUrl(): string | null {
    return this.orcid ? `https://orcid.org/${this.orcid}` : null;
  }

  get expertiseMots(): string {
    return (this.directeur?.expertiseMots ?? '').trim();
  }

  get titreChercheur(): string {
    return (this.directeur?.titre ?? '').trim();
  }

  get thesesUrl(): string {
    return `https://theses.fr/?q=${encodeURIComponent(this.nomComplet)}`;
  }

  get emailChercheur(): string | null {
    return this.offre?.directionTheseEmail ?? this.offre?.deposantEmail ?? null;
  }

  get motsClesListe(): string[] {
    const mots = this.offre?.motsCles;
    if (!mots) {
      return [];
    }
    return Object.values(mots).filter(m => !!m && m.trim() !== '');
  }

  get datePublication(): string | null {
    const raw = this.offre?.dateMiseEnLigne;
    if (!raw) {
      return null;
    }
    const m = raw.match(/(\d{4})-(\d{2})-(\d{2})/);
    if (!m) {
      return null;
    }
    const jour = parseInt(m[3], 10);
    const mois = MOIS[parseInt(m[2], 10) - 1];
    if (!mois) {
      return null;
    }
    return `${jour} ${mois} ${m[1]}`;
  }

  get texteEncadrement(): string {
    return this.offre?.resume || this.offre?.thematiqueRecherche || '';
  }

  get hasLieu(): boolean {
    return !!(this.etablissement || this.ecoleDoctorale || this.laboratoire);
  }

  /** Lieu issu du profil DT, repli sur le snapshot de l'offre (vide = absent). */
  private premierNonVide(...vals: Array<string | null | undefined>): string {
    for (const v of vals) {
      if (v && v.trim()) {
        return v.trim();
      }
    }
    return '';
  }

  get etablissement(): string {
    return this.premierNonVide(this.directeur?.etablissement, this.offre?.etablissementLibelle);
  }

  get ecoleDoctorale(): string {
    return this.premierNonVide(this.directeur?.ecoleDoctorale, this.offre?.ecoleDoctoraleLibelle);
  }

  get laboratoire(): string {
    return this.premierNonVide(this.directeur?.laboratoire, this.offre?.uniteRechercheLibelle);
  }

  get villeEtablissement(): string {
    const cp = (this.offre?.etablissementCodePostal ?? '').trim();
    const ville = (this.offre?.etablissementVille ?? '').trim();
    return `${cp} ${ville}`.trim();
  }

  /** Requête de localisation : ville si dispo, sinon nom de l'établissement. */
  get mapQuery(): string | null {
    const ville = (this.offre?.etablissementVille ?? '').trim();
    const etab = (this.offre?.etablissementLibelle ?? '').trim();
    const cp = (this.offre?.etablissementCodePostal ?? '').trim();
    if (ville) {
      return `${cp} ${ville}`.trim();
    }
    return etab || null;
  }

  get mapUrl(): SafeResourceUrl | null {
    if (this.mapLat === null || this.mapLon === null) {
      return null;
    }
    const deltaLon = 0.03;
    const deltaLat = 0.018;
    const bbox = `${this.mapLon - deltaLon},${this.mapLat - deltaLat},${this.mapLon + deltaLon},${this.mapLat + deltaLat}`;
    const url = `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${this.mapLat},${this.mapLon}`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  get mapSearchUrl(): string | null {
    if (this.mapLat === null || this.mapLon === null) {
      return null;
    }
    return `https://www.openstreetmap.org/?mlat=${this.mapLat}&mlon=${this.mapLon}#map=13/${this.mapLat}/${this.mapLon}`;
  }

  /** Géocode le lieu via Nominatim (OpenStreetMap) pour centrer la carte. */
  private geocoderLieu(): void {
    this.mapLat = null;
    this.mapLon = null;
    if (!this.mapQuery) {
      return;
    }
    const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=${encodeURIComponent(this.mapQuery)}`;
    this.http.get<Array<{ lat: string; lon: string }>>(url).subscribe({
      next: (res) => {
        if (res && res.length > 0) {
          this.mapLat = parseFloat(res[0].lat);
          this.mapLon = parseFloat(res[0].lon);
        }
      },
      error: () => {
        this.mapLat = null;
        this.mapLon = null;
      }
    });
  }

  basculerEncadrement(): void {
    this.encadrementEtendu = !this.encadrementEtendu;
  }

  prendreContact(): void {
    if (!this.offre || !this.offreId || isNaN(this.offreId)) {
      return;
    }
    this.contactContextService.setContext(
      this.offreId,
      this.offre.theseTitre ?? null,
      this.emailChercheur,
      this.offre.typeProposition ?? 'offre',
      this.offre.sujetAttribue ?? null
    );
    this.router.navigate(['/contact']);
  }
}
