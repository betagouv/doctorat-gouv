import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Title, Meta } from '@angular/platform-browser';
import { AuthService } from '../../services/auth.service';
import { InscriptionStoreService } from '../../services/inscription-store.service';
import { DemandeMiseEnRelationContextService } from '../../services/demande-mise-en-relation-context.service';
import { estSituationPartenaire } from '../../models/profil.model';

const CV_MAX_SIZE = 1 * 1024 * 1024;        // 1 Mo
const PIECE_MAX_SIZE = 5 * 1024 * 1024;     // 5 Mo
const MAX_PIECES = 5;

interface PieceSelectionnee {
  file: File;
  erreur?: string;
}

/**
 * Étape 2 de l'inscription.
 * Candidat classique : CV (PDF ≤ 1 Mo) + pièces complémentaires (PDF ≤ 5 Mo, max 5).
 * Profil partenariat : 2 documents dédiés (PDF ≤ 5 Mo) — pas de CV.
 * À la soumission, envoie un FormData à /api/inscription/complet puis redirige.
 */
@Component({
  selector: 'app-inscription-documents',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './inscription-documents.html',
  styleUrl: './inscription-documents.scss',
})
export class InscriptionDocuments implements OnInit {

  cv: File | null = null;
  cvErreur: string | null = null;
  pieces: PieceSelectionnee[] = [];
  pieceErreur: string | null = null;
  globalError: string | null = null;
  isSubmitting = false;

  /** Parcours partenariat : 2 documents dédiés au lieu de CV + pièces. */
  estPartenaire = false;
  docOrganisation: File | null = null;
  docOrganisationErreur: string | null = null;
  docPartenariat: File | null = null;
  docPartenariatErreur: string | null = null;

  constructor(
    private translate: TranslateService,
    private titleService: Title,
    private metaService: Meta,
    private store: InscriptionStoreService,
    private authService: AuthService,
    private demandeContextService: DemandeMiseEnRelationContextService,
    private router: Router,
  ) { }

  ngOnInit(): void {
    this.titleService.setTitle(
      this.translate.currentLang === 'en' ? 'Sign up — Doctorat.gouv.fr' : 'Inscription — Doctorat.gouv.fr'
    );
    this.metaService.updateTag({
      name: 'description',
      content: this.translate.currentLang === 'en'
        ? 'Complete your candidate profile by uploading your CV and supporting documents.'
        : 'Complétez votre profil de candidat en envoyant votre CV et vos pièces complémentaires.',
    });

    const coordonnees = this.store.coordonnees();
    if (!coordonnees) {
      this.router.navigate(['/inscription/coordonnees']);
      return;
    }
    this.estPartenaire = estSituationPartenaire(coordonnees.situation);
  }

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.cvErreur = null;
    const file = input.files?.[0];
    if (!file) {
      this.cv = null;
      return;
    }
    const erreur = this.validerPdf(file, CV_MAX_SIZE);
    if (erreur) {
      this.cv = null;
      this.cvErreur = erreur;
      input.value = '';
      return;
    }
    this.cv = file;
  }

  onDocOrganisationSelected(event: Event): void {
    this.docOrganisation = this.extraireFichier(event, 'docOrganisationErreur');
  }

  onDocPartenariatSelected(event: Event): void {
    this.docPartenariat = this.extraireFichier(event, 'docPartenariatErreur');
  }

  supprimerDocOrganisation(): void {
    this.docOrganisation = null;
    this.docOrganisationErreur = null;
  }

  supprimerDocPartenariat(): void {
    this.docPartenariat = null;
    this.docPartenariatErreur = null;
  }

  private extraireFichier(event: Event, champErreur: 'docOrganisationErreur' | 'docPartenariatErreur'): File | null {
    const input = event.target as HTMLInputElement;
    this[champErreur] = null;
    const file = input.files?.[0];
    if (!file) {
      return null;
    }
    const erreur = this.validerPdf(file, PIECE_MAX_SIZE);
    if (erreur) {
      this[champErreur] = erreur;
      input.value = '';
      return null;
    }
    return file;
  }

  onPiecesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.pieceErreur = null;
    const fichiers = Array.from(input.files ?? []);

    const total = this.pieces.length + fichiers.length;
    if (total > MAX_PIECES) {
      this.pieceErreur = this.translate.instant('INSCRIPTION.ERROR_PIECES_MAX', { max: MAX_PIECES });
      input.value = '';
      return;
    }

    const nouvelles = fichiers.map(f => ({ file: f, erreur: this.validerPdf(f, PIECE_MAX_SIZE) ?? undefined }));
    this.pieces = [...this.pieces, ...nouvelles];

    if (this.pieces.some(p => p.erreur)) {
      this.pieceErreur = this.translate.instant('INSCRIPTION.ERROR_PIECES_FORMAT');
    }
    input.value = '';
  }

  supprimerCv(): void {
    this.cv = null;
    this.cvErreur = null;
  }

  supprimerPiece(index: number): void {
    this.pieces.splice(index, 1);
    this.pieces = [...this.pieces];
    if (this.pieces.length === 0) {
      this.pieceErreur = null;
    }
  }

  get formulaireValide(): boolean {
    if (this.estPartenaire) {
      return !!this.docOrganisation
        && !this.docOrganisationErreur
        && !!this.docPartenariat
        && !this.docPartenariatErreur;
    }
    return !!this.cv
      && !this.cvErreur
      && this.pieces.length > 0
      && this.pieces.every(p => !p.erreur);
  }

  formatTaille(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  onSubmit(): void {
    if (!this.formulaireValide) {
      return;
    }
    const coordonnees = this.store.coordonnees();
    if (!coordonnees) {
      this.router.navigate(['/inscription/coordonnees']);
      return;
    }

    this.isSubmitting = true;
    this.globalError = null;

    const formData = new FormData();
    formData.append('coordonnees', JSON.stringify(coordonnees));
    if (this.estPartenaire) {
      formData.append('piecesComplementaires', this.docOrganisation as File);
      formData.append('piecesComplementaires', this.docPartenariat as File);
    } else {
      formData.append('cv', this.cv as File);
      this.pieces.forEach(p => formData.append('piecesComplementaires', p.file));
    }

    this.authService.inscrireComplet(formData).subscribe({
      next: () => {
        this.store.clear();
        this.demandeContextService.poserNouveauCompte();
        this.router.navigate(['/inscription/terminee']);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.globalError = err?.error?.error || this.translate.instant('INSCRIPTION.ERROR_SUBMIT');
      }
    });
  }

  private validerPdf(file: File, maxSize: number): string | null {
    const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
    if (!isPdf) {
      return this.translate.instant('INSCRIPTION.ERROR_FORMAT_PDF');
    }
    if (file.size > maxSize) {
      return this.translate.instant('INSCRIPTION.ERROR_TAILLE', { max: this.formatTaille(maxSize) });
    }
    return null;
  }
}
