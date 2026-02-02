import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
	name: 'defaultValue'
})
export class DefaultValuePipe implements PipeTransform {
	transform(value: any, fallback: string = 'Non renseigné.'): string {
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

