import { Pipe, PipeTransform } from '@angular/core';
import { SITUATIONS, SITUATIONS_LEGACY } from '../models/profil.model';

@Pipe({
	name: 'situationLibelle',
	standalone: true,
})
export class SituationLibellePipe implements PipeTransform {

	transform(value: string | null | undefined): string {
		if (!value) {
			return '—';
		}

		const option = SITUATIONS.find(o => o.value === value)
			?? SITUATIONS_LEGACY.find(o => o.value === value);
		return option?.label ?? value;
	}
}
