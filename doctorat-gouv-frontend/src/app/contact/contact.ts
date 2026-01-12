import { Component } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { NgFor } from '@angular/common';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [
	ReactiveFormsModule,
	NgFor
  ],
  templateUrl: './contact.html',
  styleUrls: ['./contact.scss']
})
export class Contact {

	contactForm!: FormGroup;

	civilites = ['Mr', 'Mme', 'Mlle'];
	profils = ['Doctorant', 'Étudiant', 'Chercheur', 'Salarié'];
	annees = [1, 2, 3, 4, 5, 6, 7, 8, 9, '> 10'];
	secteurs = [
	  'Sciences humaines',
	  'Sciences sociales',
	  'Sciences exactes',
	  'Sciences de l’ingénieur',
	  'Sciences de la vie',
	  'Sciences médicales'
	];

	constructor(private fb: FormBuilder) {
	  this.contactForm = this.fb.group({
	    nom: ['', Validators.required],
	    prenom: ['', Validators.required],
	    civilite: ['', Validators.required],
	    email: ['', [Validators.required, Validators.email]],
	    profil: ['', Validators.required],
	    annees: ['', Validators.required],
	    secteur: ['', Validators.required],
	    message: ['', Validators.required],
		
		// nouveaux champs 
		confirmMaster: [false, Validators.requiredTrue], 
		cv: [null, Validators.required], 
		documents: [null]
		
	  });
	}


  onSubmit() {
    if (this.contactForm.valid) {
      console.log('Formulaire envoyé :', this.contactForm.value);
    }
  }
}
