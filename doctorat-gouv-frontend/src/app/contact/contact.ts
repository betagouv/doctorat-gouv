import { Component } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

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
	
	private readonly apiBase = `${environment.apiUrl}`;

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

	constructor(private fb: FormBuilder, private http: HttpClient) {
	  this.contactForm = this.fb.group({
	    nom: ['', Validators.required],
	    prenom: ['', Validators.required],
	    civilite: [''],
	    email: ['', [Validators.required, Validators.email]],
	    profil: [''],
	    annees: [''],
	    secteur: [''],
	    message: [''],
		
		// nouveaux champs 
		confirmMaster: [false], 
		cv: [null], 
		documents: [null]
		
	  });
	}
	
	onSubmit() {
		console.log("SUBMIT APPELÉ !");
		console.log("Form valid:", this.contactForm.valid);
	  if (this.contactForm.valid) {
	    this.http.post(`${this.apiBase}/contact`, this.contactForm.value)
	      .subscribe(() => {
	        console.log("Email envoyé");
	      });
	  }
	}

}
