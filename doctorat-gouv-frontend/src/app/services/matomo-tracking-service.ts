import { Injectable } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

declare global {
	interface Window {
		_paq: any[];
	}
}

@Injectable({ providedIn: 'root' })
export class MatomoTrackingService {
	
	constructor(private router: Router) {
		this.router.events
			.pipe(filter(event => event instanceof NavigationEnd))
			.subscribe((event: NavigationEnd) => {
				window._paq = window._paq || [];
				window._paq.push(['setCustomUrl', event.urlAfterRedirects]);
				window._paq.push(['setDocumentTitle', document.title]);
				window._paq.push(['trackPageView']);
			});
	}
	
	trackLink(url: string, linkType: 'link' | 'download' = 'link') {
	  window._paq = window._paq || [];
	  window._paq.push(['trackLink', url, linkType]);
	}

}
