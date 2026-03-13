import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ContactContextService } from '../services/contact-context-service';
import { environment } from '../../environments/environment';
import { TranslateModule } from '@ngx-translate/core';
import { TranslateService } from '@ngx-translate/core';

import { Header } from '../header/header';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [
	CommonModule,
	ReactiveFormsModule,
	TranslateModule,
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
	
	isSubmitting = false;

	civilites = [
	  'CONTACT.CIVILITES.MONSIEUR',
	  'CONTACT.CIVILITES.MADAME',
	  'CONTACT.CIVILITES.NSP'
	];

	profils = [
	  'CONTACT.PROFILS.MASTER_FR',
	  'CONTACT.PROFILS.INGENIEUR',
	  'CONTACT.PROFILS.GRANDE_ECOLE',
	  'CONTACT.PROFILS.MASTER_ETRANGER',
	  'CONTACT.PROFILS.CHERCHEUR_ENTREPRISE',
	  'CONTACT.PROFILS.PROFESSIONNEL',
	  'CONTACT.PROFILS.ENTREPRISE_PARTENARIAT',
	  'CONTACT.PROFILS.ORGA_PARTENARIAT',
	  'CONTACT.PROFILS.AUTRE'
	];


	annees = [1, 2, 3, 4, 5, 6, 7, 8, 9, '10'];
	
	secteurs = [
	  'CONTACT.SECTEURS.AGRICULTURE',
	  'CONTACT.SECTEURS.BTP',
	  'CONTACT.SECTEURS.ENERGIE',
	  'CONTACT.SECTEURS.HOTELLERIE',
	  'CONTACT.SECTEURS.INDUSTRIE',
	  'CONTACT.SECTEURS.AGROALIMENTAIRE',
	  'CONTACT.SECTEURS.AUTOMOBILE',
	  'CONTACT.SECTEURS.PHARMA',
	  'CONTACT.SECTEURS.TEXTILE',
	  'CONTACT.SECTEURS.LUXE',
	  'CONTACT.SECTEURS.MARITIME',
	  'CONTACT.SECTEURS.NUMERIQUE',
	  'CONTACT.SECTEURS.SOINS',
	  'CONTACT.SECTEURS.TOURISME',
	  'CONTACT.SECTEURS.TRANSPORT'
	];

	constructor(private fb: FormBuilder, private http: HttpClient, private contactContextService: ContactContextService, private translate: TranslateService) {
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
		  'CONTACT.PROFILS.CHERCHEUR_ENTREPRISE',
		  'CONTACT.PROFILS.PROFESSIONNEL'
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
		  'CONTACT.PROFILS.ENTREPRISE_PARTENARIAT',
		  'CONTACT.PROFILS.ORGA_PARTENARIAT',
		  'CONTACT.PROFILS.AUTRE'
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
	  if (!id || id === 0) { 
	     this.sujetValide = false; 
	     this.sujetErreurMessage = "Aucun sujet valide n’a été sélectionné. Merci de revenir à la liste des sujets."; 
	  }

	}
	
	onSubmit() {
		
		if (this.isSubmitting) {
		  return; // Empêche tout double clic
		}

		// Vérification du contexte dès l'initialisation 
		const { id, sujet, email, typeOffre } = this.contactContextService.getContext();
		if (!id || id === 0) { 
		   this.sujetValide = false; 
		   this.sujetErreurMessage = "Aucun sujet valide n’a été sélectionné. Merci de revenir à la liste des sujets."; 
		   return; // 🔥 Très important : on bloque l’envoi si le contexte est invalide
		}

		this.contactForm.markAllAsTouched();

		if (!this.contactForm.valid) {
			console.warn("Formulaire invalide");
			return;
		}
		
		this.isSubmitting = true; // On bloque le bouton pour éviter les doubles soumissions
		
		// 🔥 Récupération des clés i18n
		const civiliteKey = this.contactForm.value.civilite;
		const profilKey = this.contactForm.value.profil;
		const secteurKey = this.contactForm.value.secteur;

		// 🔥 Traductions (labels)
		const civiliteLabel = civiliteKey ? this.translate.instant(civiliteKey) : null;
		const profilLabel = profilKey ? this.translate.instant(profilKey) : null;
		const secteurLabel = secteurKey ? this.translate.instant(secteurKey) : null;

		const payload = {
			...this.contactForm.value,
			
			// 🔥 Ajout des labels traduits
			civiliteLabel: civiliteLabel,
			profilLabel: profilLabel,
			secteurLabel: secteurLabel,
			
			// 🔥 Ajout des fichiers PJ en Base64
			cvBase64: this.cvBase64,
			documentBase64: this.documentBase64,
			
			// 🔥 Ajout des données du contexte 
			idPropositionThese: id,
			titreSujet: sujet, 
			emailEncadrant: email,
			typeOffre: typeOffre,
			
			// 🔥 Langue active (utile si le backend doit faire du traitement spécifique selon la langue)
			lang: this.translate.currentLang
		};

		this.http.post(`${this.apiBase}/contact`, payload)
		  .subscribe({
		    next: () => {
		      this.showConfirmation = true;
			  // Vider le contexte après usage
		      this.contactContextService.clear();
			  console.log("Formulaire envoyé avec succès");
		    },
		    error: () => {
		      console.error("Erreur lors de l’envoi");
		    },
		    complete: () => {
		      this.isSubmitting = false;
		    }
		  });
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
