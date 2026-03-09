import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';

@Injectable({ providedIn: 'root' })
export class I18nService {

  constructor(
    private http: HttpClient,
    private translate: TranslateService
  ) {
    this.translate.addLangs(['fr', 'en']);
    this.translate.setDefaultLang('fr');

    const saved = localStorage.getItem('lang') || 'fr';
    this.useLang(saved);
  }

  useLang(lang: string) {
	this.http.get<Record<string, any>>(`/assets/i18n/${lang}.json`).subscribe(translations => {
	  this.translate.setTranslation(lang, translations, true);
	  this.translate.use(lang);
	});
  }
}
