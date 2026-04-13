import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'nl2br',
  standalone: true
})
export class Nl2brPipe implements PipeTransform {

	transform(value: string | null): string {
	  if (!value) return '';

    let cleaned = value;

    // 1. Décodage des entités HTML comme &#92;rho → \rho
    cleaned = cleaned.replace(/&#92;/g, '\\');

    // 2. Remplacement des sauts de ligne
    cleaned = cleaned
      .replace(/\\r\\n/g, '<br>')
      .replace(/\r\n/g, '<br>')
      .replace(/\n/g, '<br>');

    return cleaned;
  }
}
