import { TestBed } from '@angular/core/testing';

import { PropositionTheseService } from './proposition-these-service';

describe('PropositionTheseService', () => {
  let service: PropositionTheseService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PropositionTheseService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
