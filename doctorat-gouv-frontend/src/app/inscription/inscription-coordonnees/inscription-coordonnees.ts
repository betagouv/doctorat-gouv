import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Title, Meta } from '@angular/platform-browser';
import { InscriptionStoreService, InscriptionCoordonnees as InscriptionCoordonneesData, Demarche, RoleSelection } from '../../services/inscription-store.service';
import { AuthService } from '../../services/auth.service';
import { InscriptionRequest } from '../../models/utilisateur.model';

@Component({
  selector: 'app-inscription-coordonnees',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './inscription-coordonnees.html',
  styleUrl: './inscription-coordonnees.scss',
})
export class InscriptionCoordonnees implements OnInit {

  form: FormGroup;

  showPassword = false;
  isSubmitting = false;
  globalError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private translate: TranslateService,
    private titleService: Title,
    private metaService: Meta,
    private store: InscriptionStoreService,
    private router: Router,
    private authService: AuthService,
  ) {
    this.form = this.fb.group({
      role: ['CANDIDAT', [Validators.required]],
      demarche: ['DOCTORAT', [Validators.required]],
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      prenom: ['', [Validators.required, Validators.maxLength(100)]],
      civilite: [''],
      situation: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      telephone: ['', [Validators.maxLength(20), Validators.pattern(/^[\d\s+().-]+$/)]],
      motDePasse: ['', [Validators.required, Validators.minLength(12)]],
      confirmationMdp: ['', [Validators.required]],
      masterConfirme: [false, [Validators.requiredTrue]],
    }, { validators: [motsDePasseIdentiques] });
  }

  ngOnInit(): void {
    const titre = this.translate.currentLang === 'en'
      ? 'Sign up — Doctorat.gouv.fr'
      : 'Inscription — Doctorat.gouv.fr';
    this.titleService.setTitle(titre);
    this.metaService.updateTag({
      name: 'description',
      content: this.translate.currentLang === 'en'
        ? 'Create an account on the national PhD platform.'
        : 'Créez un compte sur la plateforme nationale du doctorat.',
    });
  }

  get role() { return this.form.get('role'); }
  get demarche() { return this.form.get('demarche'); }
  get nom() { return this.form.get('nom'); }
  get prenom() { return this.form.get('prenom'); }
  get civilite() { return this.form.get('civilite'); }
  get situation() { return this.form.get('situation'); }
  get email() { return this.form.get('email'); }
  get telephone() { return this.form.get('telephone'); }
  get motDePasse() { return this.form.get('motDePasse'); }
  get confirmationMdp() { return this.form.get('confirmationMdp'); }
  get masterConfirme() { return this.form.get('masterConfirme'); }

  get isDirecteur(): boolean {
    return this.role?.value === 'DIRECTEUR_THESE';
  }

  onRoleChange(): void {
    if (this.isDirecteur) {
      this.situation?.clearValidators();
      this.situation?.setValue('');
      this.masterConfirme?.clearValidators();
      this.masterConfirme?.setValue(false);
      this.demarche?.clearValidators();
      this.demarche?.setValue('DOCTORAT');
    } else {
      this.situation?.setValidators([Validators.required]);
      this.situation?.markAsPristine();
      this.masterConfirme?.setValidators([Validators.requiredTrue]);
      this.masterConfirme?.markAsPristine();
      this.demarche?.setValidators([Validators.required]);
      this.demarche?.markAsPristine();
    }
    this.situation?.updateValueAndValidity();
    this.masterConfirme?.updateValueAndValidity();
    this.demarche?.updateValueAndValidity();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.value;

    if (this.isDirecteur) {
      this.inscrireDirecteur(value);
    } else {
      this.inscrireCandidat(value);
    }
  }

  private inscrireCandidat(value: any): void {
    const coordonnees: InscriptionCoordonneesData = {
      role: 'CANDIDAT',
      demarche: value.demarche as Demarche,
      nom: value.nom,
      prenom: value.prenom,
      civilite: value.civilite || undefined,
      situation: value.situation,
      email: value.email,
      telephone: value.telephone || undefined,
      motDePasse: value.motDePasse,
      masterConfirme: value.masterConfirme,
    };

    this.store.setCoordonnees(coordonnees);
    this.router.navigate(['/inscription/documents']);
  }

  private inscrireDirecteur(value: any): void {
    this.isSubmitting = true;
    this.globalError = null;

    const request: InscriptionRequest = {
      email: value.email,
      motDePasse: value.motDePasse,
      prenom: value.prenom,
      nom: value.nom,
      role: 'DIRECTEUR_THESE',
    };

    this.authService.inscription(request).subscribe({
      next: () => {
        this.store.clear();
        this.router.navigate(['/inscription/terminee']);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.globalError = err?.error?.error || 'Une erreur est survenue. Veuillez réessayer.';
      }
    });
  }
}

function motsDePasseIdentiques(group: AbstractControl): ValidationErrors | null {
  const mdp = group.get('motDePasse')?.value;
  const conf = group.get('confirmationMdp')?.value;
  return mdp && conf && mdp !== conf ? { passwordMismatch: true } : null;
}
