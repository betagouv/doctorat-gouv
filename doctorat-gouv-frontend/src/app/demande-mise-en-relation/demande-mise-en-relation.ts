import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { DemandeMiseEnRelationContextService } from '../services/demande-mise-en-relation-context.service';

@Component({
	selector: 'app-demande-mise-en-relation',
	standalone: true,
	imports: [
		CommonModule,
		ReactiveFormsModule
	],
	templateUrl: './demande-mise-en-relation.html',
	styleUrl: './demande-mise-en-relation.scss',
})
export class DemandeMiseEnRelation implements OnInit {

	demandeForm!: FormGroup;
	isSubmitting = false;

	titreSujet: string = '';
	idPropositionThese: number | null = null;

	constructor(
		private fb: FormBuilder,
		private titleService: Title,
		private router: Router,
		private authService: AuthService,
		private contextService: DemandeMiseEnRelationContextService
	) {
		this.demandeForm = this.fb.group({
			motivations: ['', [Validators.required, Validators.maxLength(3500)]],
			biaisAlgorithmiques: ['', [Validators.required]],
			rgpdConsent: [false, Validators.requiredTrue],
		});
	}

	ngOnInit(): void {
		this.titleService.setTitle('Demande de mise en relation — Doctorat.gouv.fr');

		const ctx = this.contextService.getContext();
		if (!ctx.id || ctx.id === 0) {
			this.router.navigate(['/search']);
			return;
		}

		this.idPropositionThese = ctx.id;
		this.titreSujet = ctx.titre || 'Sujet non renseigné';
	}

	get prenom(): string {
		return this.authService.currentUser()?.prenom ?? '';
	}

	get motivationsLength(): number {
		return this.demandeForm.get('motivations')?.value?.length || 0;
	}

	onSubmit(): void {
		if (this.isSubmitting) return;

		this.demandeForm.markAllAsTouched();

		if (!this.demandeForm.valid) {
			return;
		}

		this.isSubmitting = true;

		// TODO: appel API backend réel (étape 6)
		// Pour l'instant, simulation d'un envoi réussi
		setTimeout(() => {
			this.isSubmitting = false;
			// TODO: afficher écran de confirmation (étape 8)
			alert('Demande envoyée avec succès ! (bouchon)');
			this.contextService.clear();
			this.router.navigate(['/proposition'], { queryParams: { id: this.idPropositionThese } });
		}, 1500);
	}

	saveBrouillon(): void {
		const formValue = this.demandeForm.value;
		sessionStorage.setItem('demandeBrouillon', JSON.stringify(formValue));
	}
}
