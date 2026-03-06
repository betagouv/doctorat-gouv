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
	showMasterConfirmation = true;
	isOrganisationProfile = false;
	
	sujetValide = true; 
	sujetErreurMessage = "";
	
	cvBase64: string | null = null; 
	documentBase64: string | null = null;

	civilites = ['Monsieur', 'Madame', 'Ne se prononce pas'];
	profils = [
	  "Étudiant au sein d'un master français",
	  "Élève d'une école d'ingénieur",
	  "Élève d'une autre grande école conférant le grade master",
	  "Étudiant d'un master étranger",
	  "Chercheur en entreprise",
	  "Autre professionnel en activité",
	  "Entreprise souhaitant établir un partenariat",
	  "Autre organisation souhaitant établir un partenariat",
	  "Autre"
	];

	annees = [1, 2, 3, 4, 5, 6, 7, 8, 9, '10'];
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
		
		confirmMaster: [false, Validators.requiredTrue],
		cv: [null, [Validators.required]], 
		document: [null, Validators.required],

		
	  });
	  
	  this.contactForm.get('profil')?.valueChanges.subscribe(value => {

	    /* ----------------------------------------------------
	     * 1) Gestion des champs "années" et "secteur"
	     * ---------------------------------------------------- */
	    const profilsAvecExperience = [
	      "Chercheur en entreprise",
	      "Autre professionnel en activité"
	    ];

	    this.showExperienceFields = profilsAvecExperience.includes(value);

	    if (!this.showExperienceFields) {
	      this.contactForm.patchValue({
	        annees: '',
	        secteur: ''
	      });
	    }

	    /* ----------------------------------------------------
	     * 2) Gestion de la case "Master"
	     * ---------------------------------------------------- */
	    const profilsSansMaster = [
	      "Entreprise souhaitant établir un partenariat",
	      "Autre organisation souhaitant établir un partenariat",
	      "Autre"
	    ];

	    this.showMasterConfirmation = !profilsSansMaster.includes(value);
		this.isOrganisationProfile = profilsSansMaster.includes(value);


	    const confirmMasterControl = this.contactForm.get('confirmMaster');

	    if (this.showMasterConfirmation) {
	      // Le champ est visible → il doit être obligatoire
	      confirmMasterControl?.setValidators([Validators.requiredTrue]);
	    } else {
	      // Le champ est masqué → on enlève l'obligation
	      confirmMasterControl?.clearValidators();
	      confirmMasterControl?.setValue(false);
	    }

	    confirmMasterControl?.updateValueAndValidity();
	  });
	  
	  const { id, sujet, email, typeOffre } = this.contactContextService.getContext();

	  // 🔥 Vérification du contexte dès l'initialisation 
	  // const { id, sujet } = this.contactContextService.getContext(); 
	  if (!id || id === 0 || !sujet || sujet.trim().length === 0) { 
	     this.sujetValide = false; 
	     this.sujetErreurMessage = "Aucun sujet valide n’a été sélectionné. Merci de revenir à la liste des sujets."; 
	  }

	}
	
	onSubmit() {

		const { id, sujet, email, typeOffre } = this.contactContextService.getContext();
		
		// 🔥 Vérification du contexte dès l'initialisation 
		// const { id, sujet } = this.contactContextService.getContext(); 
		if (!id || id === 0 || !sujet || sujet.trim().length === 0) { 
		   this.sujetValide = false; 
		   this.sujetErreurMessage = "Aucun sujet valide n’a été sélectionné. Merci de revenir à la liste des sujets."; 
		}

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
			emailEncadrant: email,
			typeOffre: typeOffre
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

	  // Vérification du format
	  if (file.type !== 'application/pdf') {
	    this.contactForm.get('cv')?.setErrors({ invalidFormat: true });
	    this.cvBase64 = null;
	    return;
	  }

	  // Vérification de la taille (1 Mo = 1 * 1024 * 1024)
	  if (file.size > 1 * 1024 * 1024) {
	    this.contactForm.get('cv')?.setErrors({ fileTooLarge: true });
	    this.cvBase64 = null;
	    return;
	  }

	  // Conversion en Base64
	  const reader = new FileReader();
	  reader.onload = () => {
	    this.cvBase64 = (reader.result as string).split(',')[1];
	    this.contactForm.get('cv')?.setErrors(null); // fichier valide
	  };
	  reader.readAsDataURL(file);
	}


	onDocsSelected(event: any) {
	  const file = event.target.files[0];
	  if (!file) return;

	  // Vérification du format
	  if (file.type !== 'application/pdf') {
	    this.contactForm.get('document')?.setErrors({ invalidFormat: true });
	    this.documentBase64 = null;
	    return;
	  }

	  // Vérification de la taille (5 Mo)
	  if (file.size > 5 * 1024 * 1024) {
	    this.contactForm.get('document')?.setErrors({ fileTooLarge: true });
	    this.documentBase64 = null;
	    return;
	  }

	  // Conversion en Base64
	  const reader = new FileReader();
	  reader.onload = () => {
	    this.documentBase64 = (reader.result as string).split(',')[1];
	    this.contactForm.get('document')?.setErrors(null); // fichier valide
	  };
	  reader.readAsDataURL(file);
	}


}
