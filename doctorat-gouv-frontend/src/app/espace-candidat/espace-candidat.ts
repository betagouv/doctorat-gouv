import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { CandidatService, ChangementMotDePasseRequest } from '../services/candidat.service';
import { AuthService } from '../services/auth.service';
import { FichierCandidat, FrequenceNotification, ProfilResponse, ProfilUpdateRequest } from '../models/profil.model';

@Component({
  selector: 'app-espace-candidat',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
  templateUrl: './espace-candidat.html',
  styleUrl: './espace-candidat.scss',
})
export class EspaceCandidat implements OnInit {

  profil: ProfilResponse | null = null;
  profilForm: FormGroup;
  motDePasseForm: FormGroup;
  isEditing = false;
  isSaving = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;

  activeTab: 'profil' | 'motdepasse' | 'notifications' | 'alertes' = 'profil';

  newCompetence = '';
  competenceError: string | null = null;
  isAddingCompetence = false;

  fichiersError: string | null = null;
  fichiersSuccess: string | null = null;
  isSavingCv = false;
  isAddingPieces = false;
  pieceEnCoursSuppression: number | null = null;

  private static readonly CV_MAX_SIZE = 1_048_576; // 1 Mo
  private static readonly PIECE_MAX_SIZE = 5_242_880; // 5 Mo
  private static readonly MAX_PIECES = 5;

  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;
  isSavingPassword = false;
  passwordSuccessMessage: string | null = null;
  passwordErrorMessage: string | null = null;

  frequenceNotification: FrequenceNotification = 'JOURNALIERE';
  isLoadingNotifications = false;
  isSavingNotifications = false;
  notifSuccessMessage: string | null = null;
  notifErrorMessage: string | null = null;

  readonly defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" fill="%23DDDDDD"><rect width="100" height="100" rx="50"/><text x="50" y="55" text-anchor="middle" fill="%23999" font-size="40" font-family="Arial">?</text></svg>'
  );

  constructor(
    private fb: FormBuilder,
    private candidatService: CandidatService,
    private authService: AuthService,
    private titleService: Title,
  ) {
    this.profilForm = this.fb.group({
      civilite: [''],
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      prenom: ['', [Validators.required, Validators.maxLength(100)]],
      situation: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      telephone: ['', [Validators.maxLength(20), Validators.pattern(/^[\d\s+().-]+$/)]],
    });

    this.motDePasseForm = this.fb.group({
      motDePasseActuel: ['', [Validators.required]],
      nouveauMotDePasse: ['', [Validators.required, Validators.minLength(12)]],
      confirmationMotDePasse: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.titleService.setTitle('Espace candidat — Doctorat.gouv.fr');
    this.loadProfil();
    this.loadNotifications();
  }

  get prenom(): string {
    return this.profil?.prenom ?? '';
  }

  get nom(): string {
    return this.profil?.nom ?? '';
  }

  get fullName(): string {
    return `${this.prenom} ${this.nom}`;
  }

  get photoUrl(): string {
    return this.profil?.photoUrl ?? this.defaultAvatar;
  }

  get candidaturesCount(): number {
    return this.profil?.nbCandidatures ?? 0;
  }

  get competences(): string[] {
    return this.profil?.competences ?? [];
  }

  get cv(): FichierCandidat | null {
    return this.profil?.cv ?? null;
  }

  get pieces(): FichierCandidat[] {
    return this.profil?.pieces ?? [];
  }

  formatTaille(bytes: number | null | undefined): string {
    if (bytes == null) {
      return 'PDF';
    }
    const ko = bytes / 1024;
    return `PDF – ${ko.toFixed(2).replace('.', ',')} Ko`;
  }

  ouvrirCv(): void {
    this.candidatService.telechargerCv().subscribe({
      next: (blob) => this.ouvrirBlob(blob),
      error: () => {
        this.fichiersError = 'CV indisponible. Veuillez réessayer.';
      }
    });
  }

  ouvrirPiece(index: number): void {
    this.candidatService.telechargerPiece(index).subscribe({
      next: (blob) => this.ouvrirBlob(blob),
      error: () => {
        this.fichiersError = 'Pièce indisponible. Veuillez réessayer.';
      }
    });
  }

  onRemplacerCv(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files?.[0];
    input.value = '';
    if (!fichier) {
      return;
    }
    const erreur = this.validerPdf(fichier, EspaceCandidat.CV_MAX_SIZE, 'CV');
    if (erreur) {
      this.fichiersError = erreur;
      return;
    }
    this.isSavingCv = true;
    this.fichiersError = null;
    this.fichiersSuccess = null;
    this.candidatService.remplacerCv(fichier).subscribe({
      next: (data) => {
        this.profil = data;
        this.isSavingCv = false;
        this.fichiersSuccess = 'CV remplacé avec succès. L’ancien fichier a été supprimé.';
      },
      error: (err) => {
        this.isSavingCv = false;
        this.fichiersError = err.error?.message || 'Erreur lors du remplacement du CV.';
      }
    });
  }

  onAjouterPieces(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichiers = Array.from(input.files ?? []);
    input.value = '';
    if (fichiers.length === 0) {
      return;
    }
    if (this.pieces.length + fichiers.length > EspaceCandidat.MAX_PIECES) {
      this.fichiersError = `Maximum ${EspaceCandidat.MAX_PIECES} pièces complémentaires autorisées.`;
      return;
    }
    for (const fichier of fichiers) {
      const erreur = this.validerPdf(fichier, EspaceCandidat.PIECE_MAX_SIZE, 'Pièce complémentaire');
      if (erreur) {
        this.fichiersError = erreur;
        return;
      }
    }
    this.isAddingPieces = true;
    this.fichiersError = null;
    this.fichiersSuccess = null;
    this.candidatService.ajouterPieces(fichiers).subscribe({
      next: (data) => {
        this.profil = data;
        this.isAddingPieces = false;
        this.fichiersSuccess = 'Pièce(s) ajoutée(s) avec succès.';
      },
      error: (err) => {
        this.isAddingPieces = false;
        this.fichiersError = err.error?.message || 'Erreur lors de l’ajout des pièces.';
      }
    });
  }

  supprimerPiece(index: number): void {
    const piece = this.pieces[index];
    if (!piece) {
      return;
    }
    if (!confirm(`Supprimer définitivement « ${piece.filename} » ? L’ancien fichier sera supprimé du stockage.`)) {
      return;
    }
    this.pieceEnCoursSuppression = index;
    this.fichiersError = null;
    this.fichiersSuccess = null;
    this.candidatService.supprimerPiece(index).subscribe({
      next: (data) => {
        this.profil = data;
        this.pieceEnCoursSuppression = null;
        this.fichiersSuccess = 'Pièce supprimée avec succès.';
      },
      error: () => {
        this.pieceEnCoursSuppression = null;
        this.fichiersError = 'Erreur lors de la suppression de la pièce.';
      }
    });
  }

  private validerPdf(fichier: File, tailleMax: number, libelle: string): string | null {
    const nom = fichier.name.toLowerCase();
    if (fichier.type !== 'application/pdf' && !nom.endsWith('.pdf')) {
      return `${libelle} : le format PDF est requis.`;
    }
    if (fichier.size > tailleMax) {
      const maxMo = tailleMax / (1024 * 1024);
      return `${libelle} : la taille maximale est de ${maxMo} Mo.`;
    }
    return null;
  }

  private ouvrirBlob(blob: Blob): void {
    const url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
    window.open(url, '_blank');
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  }

  loadProfil(): void {
    this.candidatService.getProfil().subscribe({
      next: (data) => {
        this.profil = data;
        this.populateForm(data);
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement du profil.';
      }
    });
  }

  switchTab(tab: 'profil' | 'motdepasse' | 'notifications' | 'alertes'): void {
    this.activeTab = tab;
    this.successMessage = null;
    this.errorMessage = null;
    this.passwordSuccessMessage = null;
    this.passwordErrorMessage = null;
    this.fichiersError = null;
    this.fichiersSuccess = null;
    this.notifSuccessMessage = null;
    this.notifErrorMessage = null;
    if (tab === 'notifications') {
      this.loadNotifications();
    }
  }

  loadNotifications(): void {
    this.isLoadingNotifications = true;
    this.notifErrorMessage = null;
    this.candidatService.getNotifications().subscribe({
      next: (data) => {
        this.frequenceNotification = data.frequence ?? 'JOURNALIERE';
        this.isLoadingNotifications = false;
      },
      error: () => {
        this.isLoadingNotifications = false;
        this.notifErrorMessage = 'Erreur lors du chargement des préférences.';
      }
    });
  }

  saveNotifications(): void {
    this.isSavingNotifications = true;
    this.notifSuccessMessage = null;
    this.notifErrorMessage = null;
    this.candidatService.updateNotifications({ frequence: this.frequenceNotification }).subscribe({
      next: (data) => {
        this.frequenceNotification = data.frequence;
        this.isSavingNotifications = false;
        this.notifSuccessMessage = 'Préférences enregistrées avec succès.';
      },
      error: () => {
        this.isSavingNotifications = false;
        this.notifErrorMessage = 'Une erreur est survenue. Veuillez réessayer.';
      }
    });
  }

  startEdit(): void {
    if (this.profil) {
      this.populateForm(this.profil);
    }
    this.isEditing = true;
    this.successMessage = null;
    this.errorMessage = null;
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.errorMessage = null;
    if (this.profil) {
      this.populateForm(this.profil);
    }
  }

  saveProfil(): void {
    if (this.profilForm.invalid) {
      this.profilForm.markAllAsTouched();
      this.successMessage = null;
      this.errorMessage = 'Veuillez compléter les champs obligatoires signalés en rouge pour enregistrer.';
      setTimeout(() => {
        document.querySelector('.ec-main .fr-alert--error')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      });
      return;
    }

    this.isSaving = true;
    this.errorMessage = null;

    const request: ProfilUpdateRequest = {
      ...this.profilForm.value,
      competences: this.competences,
    };

    this.candidatService.updateProfil(request).subscribe({
      next: (data) => {
        this.profil = data;
        this.isEditing = false;
        this.isSaving = false;
        this.successMessage = 'Profil mis à jour avec succès.';
      },
      error: () => {
        this.isSaving = false;
        this.errorMessage = 'Une erreur est survenue. Veuillez réessayer.';
      }
    });
  }

  addCompetence(): void {
    const trimmed = this.newCompetence.trim();
    if (!trimmed) {
      this.competenceError = 'Veuillez saisir une compétence.';
      return;
    }
    if (this.competences.includes(trimmed)) {
      this.competenceError = 'Cette compétence existe déjà.';
      return;
    }

    this.isAddingCompetence = true;
    this.competenceError = null;

    this.candidatService.addCompetence(trimmed).subscribe({
      next: (data) => {
        this.profil = data;
        this.newCompetence = '';
        this.isAddingCompetence = false;
      },
      error: () => {
        this.isAddingCompetence = false;
        this.errorMessage = "Erreur lors de l'ajout de la compétence.";
      }
    });
  }

  removeCompetence(competence: string): void {
    this.candidatService.removeCompetence(competence).subscribe({
      next: (data) => {
        this.profil = data;
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression de la compétence.';
      }
    });
  }

  changerMotDePasse(): void {
    if (this.motDePasseForm.invalid) {
      this.motDePasseForm.markAllAsTouched();
      return;
    }

    const formValue = this.motDePasseForm.value;
    if (formValue.nouveauMotDePasse !== formValue.confirmationMotDePasse) {
      this.passwordErrorMessage = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.isSavingPassword = true;
    this.passwordErrorMessage = null;
    this.passwordSuccessMessage = null;

    const request: ChangementMotDePasseRequest = {
      motDePasseActuel: formValue.motDePasseActuel,
      nouveauMotDePasse: formValue.nouveauMotDePasse,
      confirmationMotDePasse: formValue.confirmationMotDePasse,
    };

    this.candidatService.changerMotDePasse(request).subscribe({
      next: () => {
        this.isSavingPassword = false;
        this.passwordSuccessMessage = 'Mot de passe modifié avec succès.';
        this.motDePasseForm.reset();
      },
      error: (err) => {
        this.isSavingPassword = false;
        this.passwordErrorMessage = err.error?.message || 'Une erreur est survenue. Veuillez réessayer.';
      }
    });
  }

  toggleCurrentPassword(): void {
    this.showCurrentPassword = !this.showCurrentPassword;
  }

  toggleNewPassword(): void {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onPhotoEdit(): void {
    // TODO: open photo edit module
  }

  onPhotoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img.dataset['fallback']) return;
    img.dataset['fallback'] = '1';
    img.src = this.defaultAvatar;
  }

  private populateForm(data: ProfilResponse): void {
    this.profilForm.patchValue({
      civilite: data.civilite ?? '',
      nom: data.nom,
      prenom: data.prenom,
      situation: data.situation,
      email: data.email,
      telephone: data.telephone ?? '',
    });
  }
}
