import { Pipe, PipeTransform } from '@angular/core';
import { DatePipe } from '@angular/common';
import { I18nService } from '../services/i18n-service';

@Pipe({
	name: 'dynamicDate',
	pure: false, // important : permet la mise à jour dynamique
	standalone: true
})
export class DynamicDatePipe implements PipeTransform {

	constructor(private i18n: I18nService) { }

	transform(value: any, format: string = 'mediumDate'): any {
		const locale = this.i18n.currentLocale;
		const datePipe = new DatePipe(locale);
		return datePipe.transform(value, format);
	}
}
