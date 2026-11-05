import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { CandidatService, ChangementMotDePasseRequest } from '../services/candidat.service';
import { AuthService } from '../services/auth.service';
import { FichierCandidat, FrequenceNotification, ProfilResponse, ProfilUpdateRequest, SITUATIONS, SituationOption } from '../models/profil.model';
import { SituationLibellePipe } from '../pipes/situation-libelle-pipe';

@Component({
  selector: 'app-espace-candidat',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, SituationLibellePipe, TranslateModule],
  templateUrl: './espace-candidat.html',
  styleUrl: './espace-candidat.scss',
})
export class EspaceCandidat implements OnInit, OnDestroy {

  profil: ProfilResponse | null = null;
  profilForm: FormGroup;
  motDePasseForm: FormGroup;
  isEditing = false;
  isSaving = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;

  activeTab: 'profil' | 'motdepasse' | 'notifications' | 'alertes' = 'profil';

  fichiersError: string | null = null;
  fichiersSuccess: string | null = null;
  isSavingCv = false;
  isAddingPieces = false;
  pieceEnCoursSuppression: number | null = null;
  isSavingPhoto = false;
  photoError: string | null = null;

  private static readonly CV_MAX_SIZE = 1_048_576; // 1 Mo
  private static readonly PIECE_MAX_SIZE = 5_242_880; // 5 Mo
  private static readonly MAX_PIECES = 5;
  private static readonly PHOTO_MAX_SIZE = 2_097_152; // 2 Mo
  private static readonly PHOTO_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

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

  private photoObjectUrl: string | null = null;

  constructor(
    private fb: FormBuilder,
    private candidatService: CandidatService,
    private authService: AuthService,
    private titleService: Title,
    private translate: TranslateService,
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
    this.titleService.setTitle(this.translate.instant('ESPACE_CANDIDAT.META_TITLE'));
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
    return this.photoObjectUrl ?? this.defaultAvatar;
  }

  get demandesCount(): number {
    return this.profil?.nbCandidatures ?? 0;
  }

  get demandesLabel(): string {
    return this.demandesCount > 1 ? 'demandes' : 'demande';
  }

  get situationOptions(): SituationOption[] {
    const options = [...SITUATIONS];
    const stockee = this.profilForm.get('situation')?.value;

    if (stockee && !options.some(option => option.value === stockee)) {
      options.push({ value: stockee, label: stockee });
    }

    return options;
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
        this.fichiersError = this.translate.instant('ESPACE_CANDIDAT.CV_UNAVAILABLE');
      }
    });
  }

  ouvrirPiece(index: number): void {
    this.candidatService.telechargerPiece(index).subscribe({
      next: (blob) => this.ouvrirBlob(blob),
      error: () => {
        this.fichiersError = this.translate.instant('ESPACE_CANDIDAT.PIECE_UNAVAILABLE');
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
        this.fichiersSuccess = this.translate.instant('ESPACE_CANDIDAT.CV_REPLACED_SUCCESS');
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
      this.fichiersError = this.translate.instant('ESPACE_CANDIDAT.MAX_PIECES_ERROR', { max: EspaceCandidat.MAX_PIECES });
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
        this.fichiersSuccess = this.translate.instant('ESPACE_CANDIDAT.PIECES_ADDED_SUCCESS');
      },
      error: (err) => {
        this.isAddingPieces = false;
        this.fichiersError = err.error?.message || this.translate.instant('ESPACE_CANDIDAT.PIECES_ADD_ERROR');
      }
    });
  }

  supprimerPiece(index: number): void {
    const piece = this.pieces[index];
    if (!piece) {
      return;
    }
    if (!confirm(this.translate.instant('ESPACE_CANDIDAT.DELETE_PIECE_CONFIRM', { filename: piece.filename }))) {
      return;
    }
    this.pieceEnCoursSuppression = index;
    this.fichiersError = null;
    this.fichiersSuccess = null;
    this.candidatService.supprimerPiece(index).subscribe({
      next: (data) => {
        this.profil = data;
        this.pieceEnCoursSuppression = null;
        this.fichiersSuccess = this.translate.instant('ESPACE_CANDIDAT.PIECE_DELETED_SUCCESS');
      },
      error: () => {
        this.pieceEnCoursSuppression = null;
        this.fichiersError = this.translate.instant('ESPACE_CANDIDAT.PIECE_DELETE_ERROR');
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
        this.loadPhoto();
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement du profil.';
      }
    });
  }

  private loadPhoto(): void {
    if (!this.profil?.photoUrl) {
      this.revokePhotoUrl();
      return;
    }
    this.candidatService.telechargerPhoto().subscribe({
      next: (blob) => {
        this.revokePhotoUrl();
        this.photoObjectUrl = URL.createObjectURL(blob);
      },
      error: () => {
        this.revokePhotoUrl();
      }
    });
  }

  private revokePhotoUrl(): void {
    if (this.photoObjectUrl) {
      URL.revokeObjectURL(this.photoObjectUrl);
      this.photoObjectUrl = null;
    }
  }

  ngOnDestroy(): void {
    this.revokePhotoUrl();
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
        this.notifErrorMessage = this.translate.instant('ESPACE_CANDIDAT.NOTIF_LOAD_ERROR');
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
        this.notifSuccessMessage = this.translate.instant('ESPACE_CANDIDAT.NOTIF_SAVE_SUCCESS');
      },
      error: () => {
        this.isSavingNotifications = false;
        this.notifErrorMessage = this.translate.instant('ESPACE_CANDIDAT.GENERIC_ERROR');
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
      this.errorMessage = this.translate.instant('ESPACE_CANDIDAT.PROFILE_FIELDS_REQUIRED');
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
        this.successMessage = this.translate.instant('ESPACE_CANDIDAT.PROFILE_UPDATE_SUCCESS');
      },
      error: () => {
        this.isSaving = false;
        this.errorMessage = this.translate.instant('ESPACE_CANDIDAT.GENERIC_ERROR');
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
        this.passwordSuccessMessage = this.translate.instant('ESPACE_CANDIDAT.PASSWORD_UPDATE_SUCCESS');
        this.motDePasseForm.reset();
      },
      error: (err) => {
        this.isSavingPassword = false;
        this.passwordErrorMessage = err.error?.message || this.translate.instant('ESPACE_CANDIDAT.GENERIC_ERROR');
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
    const input = document.getElementById('photo-input') as HTMLInputElement | null;
    input?.click();
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files?.[0];
    input.value = '';
    if (!fichier) {
      return;
    }
    if (!EspaceCandidat.PHOTO_TYPES.includes(fichier.type)) {
      this.photoError = this.translate.instant('ESPACE_CANDIDAT.PHOTO_INVALID_FORMAT');
      return;
    }
    if (fichier.size > EspaceCandidat.PHOTO_MAX_SIZE) {
      this.photoError = this.translate.instant('ESPACE_CANDIDAT.PHOTO_TOO_LARGE');
      return;
    }
    this.isSavingPhoto = true;
    this.photoError = null;
    this.candidatService.remplacerPhoto(fichier).subscribe({
      next: (data) => {
        this.profil = data;
        this.isSavingPhoto = false;
        this.loadPhoto();
      },
      error: (err) => {
        this.isSavingPhoto = false;
        this.photoError = err.error?.message || this.translate.instant('ESPACE_CANDIDAT.PHOTO_UPDATE_ERROR');
      }
    });
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
