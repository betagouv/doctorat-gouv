import { TestBed } from '@angular/core/testing';

import { ContactContextService } from './contact-context-service';

describe('ContactContextService', () => {
  let service: ContactContextService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ContactContextService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
