import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { CandidatService } from '../services/candidat.service';
import { AuthService } from '../services/auth.service';
import { ProfilResponse, ProfilUpdateRequest } from '../models/profil.model';

@Component({
  selector: 'app-espace-candidat',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './espace-candidat.html',
  styleUrl: './espace-candidat.scss',
})
export class EspaceCandidat implements OnInit {

  profil: ProfilResponse | null = null;
  profilForm: FormGroup;
  isEditing = false;
  isSaving = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;

  activeTab: 'profil' | 'motdepasse' | 'notifications' | 'alertes' = 'profil';

  newCompetence = '';
  competenceError: string | null = null;
  isAddingCompetence = false;

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
  }

  ngOnInit(): void {
    this.titleService.setTitle('Espace candidat — Doctorat.gouv.fr');
    this.loadProfil();
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
