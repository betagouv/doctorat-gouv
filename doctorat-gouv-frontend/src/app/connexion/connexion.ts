import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Title, Meta } from '@angular/platform-browser';

@Component({
  selector: 'app-connexion',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './connexion.html',
  styleUrl: './connexion.scss',
})
export class Connexion implements OnInit {

  loginForm: FormGroup;
  showPassword = false;
  globalError: string | null = null;
  isSubmitting = false;

  constructor(
    private fb: FormBuilder,
    private translate: TranslateService,
    private titleService: Title,
    private metaService: Meta
  ) {
    this.loginForm = this.fb.group({
      email: [''],
      password: ['']
    });
  }

  ngOnInit(): void {
    const titre = this.translate.currentLang === 'en'
      ? 'Login — Doctorat.gouv.fr'
      : 'Connexion — Doctorat.gouv.fr';
    this.titleService.setTitle(titre);

    const description = this.translate.currentLang === 'en'
      ? 'Log in to the national PhD platform with FranceConnect or your account.'
      : 'Connectez-vous à la plateforme nationale du doctorat via FranceConnect ou avec votre compte.';
    this.metaService.updateTag({ name: 'description', content: description });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    // Phase 3 — stub pour l'instant
  }
}
