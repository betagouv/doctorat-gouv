import { Injectable } from '@angular/core';

@Injectable({
	providedIn: 'root'
})
export class ContactContextService {

	private data: { id?: number; sujet?: string; email?: string, typeOffre?: string, sujetAttribue?: string } = {};
	private storageKey = 'contactContext';

	setContext(id: number | null, sujet: string | null, email: string | null, typeOffre: string | null, sujetAttribue: string | null) {
		this.data = {
			id: id ?? undefined,
			sujet: sujet ?? undefined,
			email: email ?? undefined,
			typeOffre: typeOffre ?? undefined,
			sujetAttribue: sujetAttribue ?? undefined
		};
		
		// Sauvegarde en sessionStorage 
		sessionStorage.setItem(this.storageKey, JSON.stringify(this.data));
	}

	getContext() {
		
		// 🔥 Si data est vide (cas du refresh), on recharge depuis sessionStorage 
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
