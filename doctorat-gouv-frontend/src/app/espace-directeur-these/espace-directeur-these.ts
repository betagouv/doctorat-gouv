import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { DirecteurTheseService, ChangementMotDePasseRequest } from '../services/directeur-these.service';
import { AuthService } from '../services/auth.service';
import { FilterService } from '../services/filter.service';
import { ProfilResponse, ProfilUpdateRequest } from '../models/profil.model';

@Component({
  selector: 'app-espace-directeur-these',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
  templateUrl: './espace-directeur-these.html',
  styleUrl: './espace-directeur-these.scss',
})
export class EspaceDirecteurThese implements OnInit {

  profil: ProfilResponse | null = null;
  profilForm: FormGroup;
  motDePasseForm: FormGroup;
  isEditing = false;
  isSaving = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;

  activeTab: 'profil' | 'axes' | 'parametres' | 'notifications' = 'profil';

  newAxe = '';
  axeError: string | null = null;
  isAddingAxe = false;

  etablissements: string[] = [];
  filteredEtablissements: string[] = [];
  showEtabSuggestions = false;

  laboratoires: string[] = [];
  filteredLaboratoires: string[] = [];
  showLaboSuggestions = false;

  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;
  isSavingPassword = false;
  passwordSuccessMessage: string | null = null;
  passwordErrorMessage: string | null = null;

  readonly defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" fill="%23DDDDDD"><rect width="100" height="100" rx="50"/><text x="50" y="55" text-anchor="middle" fill="%23999" font-size="40" font-family="Arial">?</text></svg>'
  );

  constructor(
    private fb: FormBuilder,
    private directeurService: DirecteurTheseService,
    private authService: AuthService,
    private filterService: FilterService,
    private titleService: Title,
  ) {
    this.profilForm = this.fb.group({
      civilite: [''],
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      prenom: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      orcid: ['', [Validators.maxLength(30)]],
      etablissement: ['', [Validators.required, Validators.maxLength(255)]],
      laboratoire: ['', [Validators.required, Validators.maxLength(255)]],
    });

    this.motDePasseForm = this.fb.group({
      motDePasseActuel: ['', [Validators.required]],
      nouveauMotDePasse: ['', [Validators.required, Validators.minLength(12)]],
      confirmationMotDePasse: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.titleService.setTitle('Espace directeur de thèse — Doctorat.gouv.fr');
    this.loadProfil();
    this.loadReferentiels();
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

  loadProfil(): void {
    this.directeurService.getProfil().subscribe({
      next: (data) => {
        this.profil = data;
        this.populateForm(data);
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement du profil.';
      }
    });
  }

  switchTab(tab: 'profil' | 'axes' | 'parametres' | 'notifications'): void {
    this.activeTab = tab;
    this.successMessage = null;
    this.errorMessage = null;
    this.passwordSuccessMessage = null;
    this.passwordErrorMessage = null;
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
      return;
    }

    this.isSaving = true;
    this.errorMessage = null;

    const request: ProfilUpdateRequest = {
      civilite: this.profilForm.value.civilite,
      nom: this.profilForm.value.nom,
      prenom: this.profilForm.value.prenom,
      situation: this.profil?.situation ?? 'Directeur de thèse',
      email: this.profilForm.value.email,
      telephone: this.profil?.telephone ?? null,
      competences: this.axes,
      orcid: this.profilForm.value.orcid || null,
      etablissement: this.profilForm.value.etablissement,
      laboratoire: this.profilForm.value.laboratoire,
    };

    this.directeurService.updateProfil(request).subscribe({
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

  get axes(): string[] {
    return this.profil?.competences ?? [];
  }

  addAxe(): void {
    const trimmed = this.newAxe.trim();
    if (!trimmed) {
      this.axeError = 'Veuillez saisir un axe de recherche.';
      return;
    }
    if (this.axes.includes(trimmed)) {
      this.axeError = 'Cet axe de recherche existe déjà.';
      return;
    }

    this.isAddingAxe = true;
    this.axeError = null;

    this.directeurService.addCompetence(trimmed).subscribe({
      next: (data) => {
        this.profil = data;
        this.newAxe = '';
        this.isAddingAxe = false;
      },
      error: () => {
        this.isAddingAxe = false;
        this.errorMessage = "Erreur lors de l'ajout de l'axe de recherche.";
      }
    });
  }

  removeAxe(axe: string): void {
    this.directeurService.removeCompetence(axe).subscribe({
      next: (data) => {
        this.profil = data;
      },
      error: () => {
        this.errorMessage = "Erreur lors de la suppression de l'axe de recherche.";
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

    this.directeurService.changerMotDePasse(request).subscribe({
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
      email: data.email,
      orcid: data.orcid ?? '',
      etablissement: data.etablissement ?? '',
      laboratoire: data.laboratoire ?? '',
    });
  }

  private loadReferentiels(): void {
    this.filterService.getAllOptions().subscribe({
      next: (options) => {
        this.etablissements = (options.ecole ?? []).filter(e => !!e && e.trim().length > 0);
        this.laboratoires = (options.laboratoire ?? []).filter(l => !!l && l.trim().length > 0);
      },
    });
  }

  filterEtablissements(): void {
    const value = (this.profilForm.get('etablissement')?.value ?? '').toLowerCase().trim();
    this.filteredEtablissements = value
      ? this.etablissements.filter(e => e.toLowerCase().includes(value)).slice(0, 8)
      : [];
    this.showEtabSuggestions = this.filteredEtablissements.length > 0;
  }

  selectEtablissement(etablissement: string): void {
    this.profilForm.get('etablissement')?.setValue(etablissement);
    this.profilForm.get('etablissement')?.markAsTouched();
    this.showEtabSuggestions = false;
  }

  hideEtabSuggestions(): void {
    setTimeout(() => { this.showEtabSuggestions = false; }, 150);
  }

  filterLaboratoires(): void {
    const value = (this.profilForm.get('laboratoire')?.value ?? '').toLowerCase().trim();
    this.filteredLaboratoires = value
      ? this.laboratoires.filter(l => l.toLowerCase().includes(value)).slice(0, 8)
      : [];
    this.showLaboSuggestions = this.filteredLaboratoires.length > 0;
  }

  selectLaboratoire(laboratoire: string): void {
    this.profilForm.get('laboratoire')?.setValue(laboratoire);
    this.profilForm.get('laboratoire')?.markAsTouched();
    this.showLaboSuggestions = false;
  }

  hideLaboSuggestions(): void {
    setTimeout(() => { this.showLaboSuggestions = false; }, 150);
  }
}
