import { Pipe, PipeTransform } from '@angular/core';
import { SITUATIONS } from '../models/profil.model';

@Pipe({
	name: 'situationLibelle',
	standalone: true,
})
export class SituationLibellePipe implements PipeTransform {

	transform(value: string | null | undefined): string {
		if (!value) {
			return '—';
		}

		return SITUATIONS.find(option => option.value === value)?.label ?? value;
	}
}
