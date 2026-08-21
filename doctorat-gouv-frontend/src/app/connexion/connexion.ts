import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Title, Meta } from '@angular/platform-browser';
import { AuthService } from '../services/auth.service';

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
    private metaService: Meta,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
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

    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/']);
    }
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  get email() {
    return this.loginForm.get('email');
  }

  get password() {
    return this.loginForm.get('password');
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.globalError = null;

    this.authService.login({
      email: this.loginForm.value.email,
      motDePasse: this.loginForm.value.password
    }).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.isSubmitting = false;
        if (err.status === 401 || err.status === 400) {
          this.globalError = 'Email ou mot de passe incorrect';
        } else {
          this.globalError = 'Service temporairement indisponible. Veuillez réessayer.';
        }
      }
    });
  }
}
