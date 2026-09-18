import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { DirecteurTheseService, ChangementMotDePasseRequest } from '../services/directeur-these.service';
import { AuthService } from '../services/auth.service';
import { FilterService } from '../services/filter.service';
import { ProfilDtResponse, ProfilDtUpdateRequest } from '../models/profil.model';
import { AxeDeRecherche } from '../models/profil.model';

@Component({
  selector: 'app-espace-directeur-these',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
  templateUrl: './espace-directeur-these.html',
  styleUrl: './espace-directeur-these.scss',
})
export class EspaceDirecteurThese implements OnInit {

  profil: ProfilDtResponse | null = null;
  profilForm: FormGroup;
  motDePasseForm: FormGroup;
  axesForm: FormGroup;
  isEditing = false;
  isSaving = false;
  isEditingAxes = false;
  isSavingAxes = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;

  activeTab: 'profil' | 'axes' | 'parametres' | 'notifications' = 'profil';

  etablissements: string[] = [];
  filteredEtablissements: string[] = [];
  showEtabSuggestions = false;

  laboratoires: string[] = [];
  filteredLaboratoires: string[] = [];
  showLaboSuggestions = false;

  ecolesDoctorales: string[] = [];
  filteredEcolesDoctorales: string[] = [];
  showEcoleSuggestions = false;

  newMotCle = '';
  motsCles: string[] = [];

  descriptionAxes = '';
  axes: AxeDeRecherche[] = [];

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
      ecoleDoctorale: ['', [Validators.required, Validators.maxLength(255)]],
      employeur: ['', [Validators.maxLength(255)]],
      titre: [''],
      precisionTitre: ['', [Validators.maxLength(255)]],
      habilitationRecherche: [''],
      domaineScientifique: ['', [Validators.required]],
      expertiseMots: ['', [Validators.required]],
    });

    this.motDePasseForm = this.fb.group({
      motDePasseActuel: ['', [Validators.required]],
      nouveauMotDePasse: ['', [Validators.required, Validators.minLength(12)]],
      confirmationMotDePasse: ['', [Validators.required]],
    });

    this.axesForm = this.fb.group({
      descriptionAxes: ['', [Validators.required, Validators.maxLength(2000)]],
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

    const request: ProfilDtUpdateRequest = {
      civilite: this.profilForm.value.civilite,
      nom: this.profilForm.value.nom,
      prenom: this.profilForm.value.prenom,
      email: this.profilForm.value.email,
      competences: this.profil?.competences ?? [],
      orcid: this.profilForm.value.orcid || null,
      etablissement: this.profilForm.value.etablissement,
      laboratoire: this.profilForm.value.laboratoire,
      ecoleDoctorale: this.profilForm.value.ecoleDoctorale || null,
      employeur: this.profilForm.value.employeur || null,
      titre: this.profilForm.value.titre || null,
      precisionTitre: this.profilForm.value.precisionTitre || null,
      habilitationRecherche: this.profilForm.value.habilitationRecherche || null,
      domaineScientifique: this.profilForm.value.domaineScientifique || null,
      expertiseMots: this.profilForm.value.expertiseMots || null,
      motsCles: this.motsCles,
      descriptionAxes: this.profil?.descriptionAxes ?? null,
      axes: (this.profil?.axes ?? []).map(a => ({titre: a.titre, precisions: a.precisions, contactable: a.contactable})),
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

  startEditAxes(): void {
    if (this.profil) {
      this.descriptionAxes = this.profil.descriptionAxes ?? '';
      this.axes = this.profil.axes ? this.profil.axes.map(a => ({...a})) : [];
      this.axesForm.patchValue({ descriptionAxes: this.descriptionAxes });
    }
    this.isEditingAxes = true;
    this.successMessage = null;
    this.errorMessage = null;
  }

  cancelEditAxes(): void {
    this.isEditingAxes = false;
    this.errorMessage = null;
    if (this.profil) {
      this.descriptionAxes = this.profil.descriptionAxes ?? '';
      this.axes = this.profil.axes ? this.profil.axes.map(a => ({...a})) : [];
      this.axesForm.patchValue({ descriptionAxes: this.descriptionAxes });
    }
  }

  saveAxes(): void {
    if (this.axesForm.invalid) {
      this.axesForm.markAllAsTouched();
      this.errorMessage = 'La description des axes de recherche est obligatoire.';
      return;
    }
    const invalidAxe = this.axes.some(a => !a.titre?.trim() || !a.precisions?.trim());
    if (invalidAxe) {
      this.errorMessage = 'Chaque axe de recherche doit avoir un titre et des précisions.';
      return;
    }
    if (!this.profil) {
      this.errorMessage = 'Profil introuvable. Veuillez recharger la page.';
      return;
    }

    this.isSavingAxes = true;
    this.errorMessage = null;

    const p = this.profil;
    const request: ProfilDtUpdateRequest = {
      civilite: p.civilite,
      nom: p.nom,
      prenom: p.prenom,
      email: p.email,
      competences: p.competences ?? [],
      orcid: p.orcid,
      etablissement: p.etablissement,
      laboratoire: p.laboratoire,
      ecoleDoctorale: p.ecoleDoctorale,
      employeur: p.employeur,
      titre: p.titre,
      precisionTitre: p.precisionTitre,
      habilitationRecherche: p.habilitationRecherche,
      domaineScientifique: p.domaineScientifique,
      expertiseMots: p.expertiseMots,
      motsCles: p.motsCles ?? [],
      descriptionAxes: (this.axesForm.value.descriptionAxes ?? '').trim() || null,
      axes: this.axes.map(a => ({titre: a.titre.trim(), precisions: a.precisions.trim(), contactable: a.contactable})),
    };

    this.directeurService.updateProfil(request).subscribe({
      next: (data) => {
        this.profil = data;
        this.descriptionAxes = data.descriptionAxes ?? '';
        this.axes = data.axes ? data.axes.map(a => ({...a})) : [];
        this.axesForm.patchValue({ descriptionAxes: this.descriptionAxes });
        this.isEditingAxes = false;
        this.isSavingAxes = false;
        this.successMessage = 'Axes de recherche mis à jour avec succès.';
      },
      error: () => {
        this.isSavingAxes = false;
        this.errorMessage = 'Une erreur est survenue. Veuillez réessayer.';
      }
    });
  }

  addAxe(): void {
    this.axes.push({ titre: '', precisions: '', contactable: false });
  }

  removeAxe(index: number): void {
    this.axes.splice(index, 1);
  }

  toggleAxeContactable(index: number): void {
    this.axes[index].contactable = !this.axes[index].contactable;
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

  private populateForm(data: ProfilDtResponse): void {
    this.profilForm.patchValue({
      civilite: data.civilite ?? '',
      nom: data.nom,
      prenom: data.prenom,
      email: data.email,
      orcid: data.orcid ?? '',
      etablissement: data.etablissement ?? '',
      laboratoire: data.laboratoire ?? '',
      ecoleDoctorale: data.ecoleDoctorale ?? '',
      employeur: data.employeur ?? '',
      titre: data.titre ?? '',
      precisionTitre: data.precisionTitre ?? '',
      habilitationRecherche: data.habilitationRecherche ?? '',
      domaineScientifique: data.domaineScientifique ?? '',
      expertiseMots: data.expertiseMots ?? '',
    });
    this.motsCles = data.motsCles ? [...data.motsCles] : [];
    this.descriptionAxes = data.descriptionAxes ?? '';
    this.axes = data.axes ? data.axes.map(a => ({...a})) : [];
    this.axesForm.patchValue({ descriptionAxes: data.descriptionAxes ?? '' });
  }

  private loadReferentiels(): void {
    this.filterService.getAllOptions().subscribe({
      next: (options) => {
        this.etablissements = (options.ecole ?? []).filter(e => !!e && e.trim().length > 0);
        this.laboratoires = (options.laboratoire ?? []).filter(l => !!l && l.trim().length > 0);
        this.ecolesDoctorales = (options.ecole ?? []).filter(e => !!e && e.trim().length > 0);
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

  filterEcolesDoctorales(): void {
    const value = (this.profilForm.get('ecoleDoctorale')?.value ?? '').toLowerCase().trim();
    this.filteredEcolesDoctorales = value
      ? this.ecolesDoctorales.filter(e => e.toLowerCase().includes(value)).slice(0, 8)
      : [];
    this.showEcoleSuggestions = this.filteredEcolesDoctorales.length > 0;
  }

  selectEcoleDoctorale(ecole: string): void {
    this.profilForm.get('ecoleDoctorale')?.setValue(ecole);
    this.profilForm.get('ecoleDoctorale')?.markAsTouched();
    this.showEcoleSuggestions = false;
  }

  hideEcoleSuggestions(): void {
    setTimeout(() => { this.showEcoleSuggestions = false; }, 150);
  }

  addMotCle(): void {
    const trimmed = this.newMotCle.trim();
    if (trimmed && !this.motsCles.includes(trimmed)) {
      this.motsCles.push(trimmed);
      this.newMotCle = '';
    }
  }

  removeMotCle(mot: string): void {
    this.motsCles = this.motsCles.filter(m => m !== mot);
  }
}
