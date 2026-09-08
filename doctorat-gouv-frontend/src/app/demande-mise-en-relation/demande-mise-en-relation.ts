import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { DemandeMiseEnRelationContextService } from '../services/demande-mise-en-relation-context.service';
import { environment } from '../../environments/environment';

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

	private readonly apiUrl = environment.apiUrl;

	demandeForm!: FormGroup;
	isSubmitting = false;
	errorMessage: string | null = null;
	brouillonSaved = false;

	titreSujet: string = '';
	idPropositionThese: number | null = null;

	constructor(
		private fb: FormBuilder,
		private titleService: Title,
		private router: Router,
		private http: HttpClient,
		private authService: AuthService,
		private contextService: DemandeMiseEnRelationContextService
	) {
		this.demandeForm = this.fb.group({
			motivations: ['', [Validators.required, Validators.maxLength(3500)]],
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

		this.loadBrouillon();
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
		this.errorMessage = null;

		const payload = {
			idPropositionThese: this.idPropositionThese,
			motivations: this.demandeForm.value.motivations,
			rgpdConsent: this.demandeForm.value.rgpdConsent,
		};

		this.http.post(`${this.apiUrl}/candidat/demande-mise-en-relation`, payload)
			.subscribe({
				next: () => {
					this.isSubmitting = false;
					this.contextService.clear();
					this.router.navigate(['/proposition'], { queryParams: { id: this.idPropositionThese } });
				},
				error: (err) => {
					this.isSubmitting = false;
					this.errorMessage = err.error?.message || 'Une erreur est survenue. Veuillez réessayer.';
				}
			});
	}

	saveBrouillon(): void {
		const formValue = this.demandeForm.value;
		const brouillon = {
			idPropositionThese: this.idPropositionThese,
			motivations: formValue.motivations,
			rgpdConsent: formValue.rgpdConsent,
		};
		sessionStorage.setItem('demandeBrouillon', JSON.stringify(brouillon));

		this.brouillonSaved = true;
		setTimeout(() => {
			this.brouillonSaved = false;
		}, 3000);
	}

	private loadBrouillon(): void {
		const saved = sessionStorage.getItem('demandeBrouillon');
		if (!saved) return;

		try {
			const brouillon = JSON.parse(saved);
			if (brouillon.idPropositionThese === this.idPropositionThese) {
				this.demandeForm.patchValue({
					motivations: brouillon.motivations || '',
					rgpdConsent: brouillon.rgpdConsent || false,
				});
			}
		} catch {
			// brouillon corrompu, on ignore
		}
	}
}
