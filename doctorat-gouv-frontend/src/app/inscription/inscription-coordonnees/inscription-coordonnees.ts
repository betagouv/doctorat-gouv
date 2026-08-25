import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Title, Meta } from '@angular/platform-browser';
import { InscriptionStoreService, InscriptionCoordonnees as InscriptionCoordonneesData, Demarche } from '../../services/inscription-store.service';

@Component({
  selector: 'app-inscription-coordonnees',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './inscription-coordonnees.html',
  styleUrl: './inscription-coordonnees.scss',
})
export class InscriptionCoordonnees implements OnInit {

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private translate: TranslateService,
    private titleService: Title,
    private metaService: Meta,
    private store: InscriptionStoreService,
    private router: Router,
  ) {
    this.form = this.fb.group({
      demarche: ['DOCTORAT', [Validators.required]],
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      prenom: ['', [Validators.required, Validators.maxLength(100)]],
      civilite: [''],
      situation: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      telephone: ['', [Validators.maxLength(20), Validators.pattern(/^[\d\s+().-]+$/)]],
      masterConfirme: [false, [Validators.requiredTrue]],
    });
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

  get demarche() { return this.form.get('demarche'); }
  get nom() { return this.form.get('nom'); }
  get prenom() { return this.form.get('prenom'); }
  get civilite() { return this.form.get('civilite'); }
  get situation() { return this.form.get('situation'); }
  get email() { return this.form.get('email'); }
  get telephone() { return this.form.get('telephone'); }
  get masterConfirme() { return this.form.get('masterConfirme'); }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.value;
    const coordonnees: InscriptionCoordonneesData = {
      demarche: value.demarche as Demarche,
      nom: value.nom,
      prenom: value.prenom,
      civilite: value.civilite || undefined,
      situation: value.situation,
      email: value.email,
      telephone: value.telephone || undefined,
      masterConfirme: value.masterConfirme,
    };

    this.store.setCoordonnees(coordonnees);
    this.router.navigate(['/inscription/documents']);
  }
}
