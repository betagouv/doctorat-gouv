import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Title } from '@angular/platform-browser';
import { CandidatService } from '../services/candidat.service';
import { AuthService } from '../services/auth.service';
import { ProfilResponse, ProfilUpdateRequest } from '../models/profil.model';

@Component({
  selector: 'app-espace-candidat',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
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

  constructor(
    private fb: FormBuilder,
    private candidatService: CandidatService,
    private authService: AuthService,
    private translate: TranslateService,
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
    this.titleService.setTitle(
      this.translate.currentLang === 'en' ? 'Candidate space — Doctorat.gouv.fr' : 'Espace candidat — Doctorat.gouv.fr'
    );
    this.loadProfil();
  }

  get prenom(): string {
    return this.profil?.prenom ?? '';
  }

  get nom(): string {
    return this.profil?.nom ?? '';
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

    const request: ProfilUpdateRequest = this.profilForm.value;

    this.candidatService.updateProfil(request).subscribe({
      next: (data) => {
        this.profil = data;
        this.isEditing = false;
        this.isSaving = false;
        this.successMessage = this.translate.currentLang === 'en'
          ? 'Profile updated successfully.'
          : 'Profil mis à jour avec succès.';
      },
      error: () => {
        this.isSaving = false;
        this.errorMessage = this.translate.currentLang === 'en'
          ? 'An error occurred. Please try again.'
          : 'Une erreur est survenue. Veuillez réessayer.';
      }
    });
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
