import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ContactContextService } from '../services/contact-context-service';
import { environment } from '../../environments/environment';

import { Header } from '../header/header';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [
	CommonModule,
	ReactiveFormsModule,
	Header
  ],
  templateUrl: './contact.html',
  styleUrls: ['./contact.scss']
})
export class Contact {
	
	private readonly apiBase = `${environment.apiUrl}`;

	contactForm!: FormGroup;
	showExperienceFields = false;
	showConfirmation = false;
	
	cvBase64: string | null = null; 
	documentBase64: string | null = null;

	civilites = ['Monsieur', 'Madame', 'Ne se prononce pas'];
	profils = [
	  'Etudiant au sein d\'un master français',
	  'Chercheur en entreprise',
	  'Autre professionel en activité',
	  'Elève d\'une autre école conférant le grade de master',
	  'Etudiant d\'un master étranger',
	  'Autre'
	];

	annees = [1, 2, 3, 4, 5, 6, 7, 8, 9, '> 10'];
	secteurs = [
		'Agriculture',
		'Bâtiment - Travaux publics',
		'Énergie',
		'Hôtellerie - Restauration, Tourisme',
		'Industrie',
		'Industrie agroalimentaire',
		'Industrie automobile',
		'Industrie pharmaceutique',
		'Industrie textile',
		'Luxe',
		'Maritime et fluvial',
		'Numérique',
		'Soin et accompagnement',
		'Tourisme',
		'Transport - Logistique'
	];


	constructor(private fb: FormBuilder, private http: HttpClient, private contactContextService: ContactContextService) {
	  this.contactForm = this.fb.group({
        nom: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(30)]], 
		prenom: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(30)]],
	    civilite: [''],
	    email: ['', [Validators.required, Validators.email]],
	    profil: [''],
	    annees: [''],
	    secteur: [''],
	    message: [''],
		rgpdConsent: [false, Validators.requiredTrue],
		
		confirmMaster: [false], 
		cv: [null, [Validators.required]], 
		document: [null]
		
	  });
	  
	  this.contactForm.get('profil')?.valueChanges.subscribe(value => {
	    this.showExperienceFields = (value === 'Chercheur en entreprise' || value === 'Autre professionel en activité');

	    if (!this.showExperienceFields) {
	      this.contactForm.patchValue({
	        annees: '',
	        secteur: ''
	      });
	    }
	  });

	  
	}
	
	onSubmit() {

		const { id, sujet, email } = this.contactContextService.getContext();

		this.contactForm.markAllAsTouched();

		if (!this.contactForm.valid) {
			console.warn("Formulaire invalide");
			return;
		}

		const payload = {
			...this.contactForm.value,
			cvBase64: this.cvBase64,
			documentBase64: this.documentBase64,
			// 🔥 Ajout des données du contexte 
			idPropositionThese: id,
			titreSujet: sujet, 
			emailEncadrant: email
		};

		this.http.post(`${this.apiBase}/contact`, payload)
			.subscribe(() => {
				this.showConfirmation = true;
				console.log("Email envoyé")
			});

		// Vider le contexte après usage 
		this.contactContextService.clear();
	}


	onCvSelected(event: any) {
	  const file = event.target.files[0];
	  if (!file) return;

	  const reader = new FileReader();
	  reader.onload = () => {
	    this.cvBase64 = (reader.result as string).split(',')[1]; // enlever le prefixe data:
	  };
	  reader.readAsDataURL(file);
	}

	onDocsSelected(event: any) {
		const file = event.target.files[0];
		if (!file) return;

		const reader = new FileReader();
		reader.onload = () => {
		  this.documentBase64 = (reader.result as string).split(',')[1]; // enlever le prefixe data:
		};
		reader.readAsDataURL(file);
	}

}
