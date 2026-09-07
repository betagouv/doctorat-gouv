import { Injectable } from '@angular/core';

@Injectable({
	providedIn: 'root'
})
export class DemandeMiseEnRelationContextService {

	private data: { id?: number; titre?: string; typeProposition?: string } = {};
	private storageKey = 'demandeMiseEnRelationContext';

	setContext(id: number | null, titre: string | null, typeProposition: string | null) {
		this.data = {
			id: id ?? undefined,
			titre: titre ?? undefined,
			typeProposition: typeProposition ?? undefined
		};

		sessionStorage.setItem(this.storageKey, JSON.stringify(this.data));
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

	clear() {
		this.data = {};
		sessionStorage.removeItem(this.storageKey);
	}
}
