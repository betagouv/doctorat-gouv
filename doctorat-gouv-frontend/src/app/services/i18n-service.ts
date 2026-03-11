import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { ApplicationRef } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class I18nService {
	
  public currentLocale = 'fr-FR';

  constructor(
    private http: HttpClient,
    private translate: TranslateService,
	private applicationRef: ApplicationRef
  ) {
    this.translate.addLangs(['fr', 'en']);
    this.translate.setDefaultLang('fr');

    const saved = localStorage.getItem('lang') || 'fr';
    this.useLang(saved);
  }
  
  useLang(lang: string) {
	
	this.currentLocale = lang === 'en' ? 'en-US' : 'fr-FR';
	this.applicationRef.tick();
	
    // const files = ['shared', 'header', 'search', 'detail', 'contact', 'footer'];
	const files = ['header', 'search', 'detail'];
    let loaded = 0;

    files.forEach(file => {
      this.http.get<Record<string, any>>(`/assets/i18n/${file}.${lang}.json`)
        .subscribe(translations => {
          this.translate.setTranslation(lang, translations, true);
          loaded++;

          if (loaded === files.length) {
            this.translate.use(lang);
            localStorage.setItem('lang', lang);
          }
        });
    });
  }

}
