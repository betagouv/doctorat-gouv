import { Injectable } from '@angular/core';

@Injectable({
	providedIn: 'root'
})
export class ContactContextService {

	private data: { id?: number; sujet?: string; email?: string, typeOffre?: string } = {};

	setContext(id: number | null, sujet: string | null, email: string | null, typeOffre: string | null) {
		this.data = {
			id: id ?? undefined,
			sujet: sujet ?? undefined,
			email: email ?? undefined,
			typeOffre: typeOffre ?? undefined
		};
	}

	getContext() {
		return this.data;
	}

	clear() {
		this.data = {};
	}
}
