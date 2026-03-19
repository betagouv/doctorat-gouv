import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Pipe({
	name: 'defaultValue',
	standalone: true,
	pure: false // important pour réagir au changement de langue
})
export class DefaultValuePipe implements PipeTransform {
	
	private lastFallbackKey = '';
	private lastFallbackValue = '';
	
	constructor(private translate: TranslateService) {}
	
	transform(value: any, fallbackKey: string = 'DETAIL.NON_RENSEIGNE'): string {
		
		// Si la clé a changé ou si la langue a changé, on recharge la traduction
		if (fallbackKey !== this.lastFallbackKey || this.translate.currentLang !== this.lastFallbackValue) {
		  this.lastFallbackKey = fallbackKey;

		  this.translate.get(fallbackKey).subscribe(translated => {
		    this.lastFallbackValue = translated;
		  });
		}

		const fallback = this.lastFallbackValue || fallbackKey;
		
		if (value === null || value === undefined) {
			return fallback;
		}

		// si c'est une chaîne vide ou uniquement des espaces
		if (typeof value === 'string' && value.trim().length === 0) {
			return fallback;
		}

		return value;
	}
}

