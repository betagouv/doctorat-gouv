import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Title, Meta } from '@angular/platform-browser';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-inscription',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './inscription.html',
  styleUrl: './inscription.scss',
})
export class Inscription implements OnInit {

  inscriptionForm: FormGroup;
  showPassword = false;
  globalError: string | null = null;
  isSubmitting = false;

  constructor(
    private fb: FormBuilder,
    private translate: TranslateService,
    private titleService: Title,
    private metaService: Meta,
    private authService: AuthService,
    private router: Router,
  ) {
    this.inscriptionForm = this.fb.group({
      prenom: ['', [Validators.required]],
      nom: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      motDePasse: ['', [Validators.required, Validators.minLength(12)]],
      confirmationMotDePasse: ['', [Validators.required]],
      role: ['CANDIDAT', [Validators.required]],
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    const titre = this.translate.currentLang === 'en'
      ? 'Sign up — Doctorat.gouv.fr'
      : 'Inscription — Doctorat.gouv.fr';
    this.titleService.setTitle(titre);

    const description = this.translate.currentLang === 'en'
      ? 'Create an account on the national PhD platform.'
      : 'Créez un compte sur la plateforme nationale du doctorat.';
    this.metaService.updateTag({ name: 'description', content: description });

    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/']);
    }
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const motDePasse = control.get('motDePasse');
    const confirmation = control.get('confirmationMotDePasse');
    if (motDePasse && confirmation && motDePasse.value !== confirmation.value) {
      confirmation.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  get prenom() { return this.inscriptionForm.get('prenom'); }
  get nom() { return this.inscriptionForm.get('nom'); }
  get email() { return this.inscriptionForm.get('email'); }
  get motDePasse() { return this.inscriptionForm.get('motDePasse'); }
  get confirmationMotDePasse() { return this.inscriptionForm.get('confirmationMotDePasse'); }
  get role() { return this.inscriptionForm.get('role'); }

  onSubmit(): void {
    if (this.inscriptionForm.invalid) {
      this.inscriptionForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.globalError = null;

    const { confirmationMotDePasse, ...request } = this.inscriptionForm.value;

    this.authService.inscription(request).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.isSubmitting = false;
        if (err.status === 400) {
          const body = err.error;
          if (body?.error) {
            this.globalError = body.error;
          } else if (body?.errors?.length) {
            this.globalError = body.errors.map((e: any) => e.defaultMessage).join('. ');
          } else if (body?.message) {
            this.globalError = body.message;
          } else if (typeof body === 'string') {
            this.globalError = body;
          } else {
            this.globalError = 'Données invalides. Veuillez vérifier le formulaire.';
          }
        } else {
          this.globalError = 'Service temporairement indisponible. Veuillez réessayer.';
        }
      }
    });
  }
}
