import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { DsfrHeaderModule } from '@edugouvfr/ngx-dsfr';
import { DsfrTagModule } from '@edugouvfr/ngx-dsfr';
import { DsfrFooterModule } from '@edugouvfr/ngx-dsfr';
import { DsfrButtonModule } from '@edugouvfr/ngx-dsfr';

@Component({
	selector: 'app-home',
	imports: [
		DsfrHeaderModule,
		DsfrTagModule,
		DsfrFooterModule,
		DsfrButtonModule
	],
	templateUrl: './home.html',
	styleUrls: ['./home.scss'],
	standalone: true
})
export class Home {
	
	constructor(private router: Router) {}

	onSearch(event: Event) {
		event.preventDefault(); // empêche le rechargement de page
		const input = (event.target as HTMLFormElement).querySelector<HTMLInputElement>('#search');
		const query = input?.value.trim();
		if (query) {
			// Exemple : redirection vers une page de résultats
			this.router.navigate(['/recherche'], { queryParams: { q: query } });
		}
	}

}
