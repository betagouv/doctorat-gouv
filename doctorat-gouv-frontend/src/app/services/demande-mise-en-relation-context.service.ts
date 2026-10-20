import { Injectable } from '@angular/core';

interface ContexteDemande {
	id?: number;
	titre?: string;
	typeProposition?: string;
	demandeEnCours?: boolean;
	nouveauCompte?: boolean;
}

@Injectable({
	providedIn: 'root'
})
export class DemandeMiseEnRelationContextService {

	private data: ContexteDemande = {};
	private storageKey = 'demandeMiseEnRelationContext';

	setContext(id: number | null, titre: string | null, typeProposition: string | null, demandeEnCours = false) {
		this.data = {
			id: id ?? undefined,
			titre: titre ?? undefined,
			typeProposition: typeProposition ?? undefined,
			demandeEnCours: demandeEnCours || undefined
		};

		this.persister();
	}

	getContext() {
		if (!this.data || Object.keys(this.data).length === 0) {
			const saved = sessionStorage.getItem(this.storageKey);
			if (saved) {
				this.data = JSON.parse(saved);
			}
		}

		return this.data;
	}

	estDemandeEnCours(): boolean {
		return !!this.getContext().demandeEnCours;
	}

	poserNouveauCompte(): void {
		const ctx = this.getContext();
		if (!ctx.demandeEnCours) {
			return;
		}

		this.data = { ...ctx, nouveauCompte: true };
		this.persister();
	}

	consommerFelicitations(): boolean {
		const ctx = this.getContext();
		if (!ctx.nouveauCompte) {
			return false;
		}

		this.data = { ...ctx, nouveauCompte: false };
		this.persister();

		return true;
	}

	clear() {
		this.data = {};
		sessionStorage.removeItem(this.storageKey);
	}

	private persister() {
		sessionStorage.setItem(this.storageKey, JSON.stringify(this.data));
	}
}
