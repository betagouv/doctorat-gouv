import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

import { registerLocaleData } from '@angular/common';

import localeFr from '@angular/common/locales/fr';
import localeEn from '@angular/common/locales/en';
import { LOCALE_ID } from '@angular/core';
import { I18nService } from './app/services/i18n-service';


registerLocaleData(localeFr);
registerLocaleData(localeEn);

bootstrapApplication(App, {
  ...appConfig,
  providers: [
    ...(appConfig.providers || []),
	{
	  provide: LOCALE_ID,
	  deps: [I18nService],
	  useFactory: (i18n: I18nService) => i18n.currentLocale
	}

  ]
})
.catch((err) => console.error(err));
