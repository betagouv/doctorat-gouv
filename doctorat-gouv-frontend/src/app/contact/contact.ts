import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [
	CommonModule,
	ReactiveFormsModule
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
        nom: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(30)]], 
		prenom: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(30)]],
	    civilite: [''],
	    email: ['', [Validators.required, Validators.email]],
	    profil: [''],
	    annees: [''],
	    secteur: [''],
	    message: [''],
		
		confirmMaster: [false], 
		cv: [null, [Validators.required]], 
		document: [null]
		
	  });
	  
	  this.contactForm.get('profil')?.valueChanges.subscribe(value => {
	    this.showExperienceFields = (value === 'Salarié' || value === 'Chercheur');

	    if (!this.showExperienceFields) {
	      this.contactForm.patchValue({
	        annees: '',
	        secteur: ''
	      });
	    }
	  });

	  
	}
	
	onSubmitOld() {
	  this.contactForm.markAllAsTouched();

	  if (!this.contactForm.valid) {
	    console.warn("Formulaire invalide");
	    return;
	  }

	  const payload = {
	    ...this.contactForm.value,
	    cvBase64: this.cvBase64,
	    documentBase64: this.documentBase64,
	  };

	  this.http.post(`${this.apiBase}/contact`, payload)
	    .subscribe(() => console.log("Email envoyé"));
	}
	
	onSubmit() {
	  this.contactForm.markAllAsTouched();

	  if (!this.contactForm.valid) {
		console.warn("Formulaire invalide");
	    return;
	  }

	  const payload = {
	    ...this.contactForm.value,
	    cvBase64: this.cvBase64,
	    documentBase64: this.documentBase64,
	  };

	  this.http.post(`${this.apiBase}/contact`, payload)
	    .subscribe(() => {
	      this.showConfirmation = true;
		  console.log("Email envoyé")
	    });
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
