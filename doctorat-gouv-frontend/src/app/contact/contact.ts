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
	
	cvBase64: string | null = null; 
	documentBase64: string | null = null;

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
		document: [null]
		
	  });
	}
	
	onSubmit() {
	  if (this.contactForm.valid) {

	    const payload = {
	      ...this.contactForm.value,
	      cvBase64: this.cvBase64,
	      documentBase64: this.documentBase64,
	    };

	    this.http.post(`${this.apiBase}/contact`, payload)
	      .subscribe(() => console.log("Email envoyé"));
	  }
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
