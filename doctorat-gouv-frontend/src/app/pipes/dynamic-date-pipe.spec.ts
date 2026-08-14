import { DynamicDatePipe } from './dynamic-date-pipe';
import { I18nService } from '../services/i18n-service';

describe('DynamicDatePipe', () => {
  it('create an instance', () => {
    const i18n = { currentLocale: 'fr-FR' } as unknown as I18nService;
    const pipe = new DynamicDatePipe(i18n);
    expect(pipe).toBeTruthy();
  });
});
