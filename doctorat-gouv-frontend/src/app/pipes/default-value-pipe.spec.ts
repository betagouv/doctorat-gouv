import { TestBed } from '@angular/core/testing';

import { DefaultValuePipe } from './default-value-pipe';

describe('DefaultValuePipe', () => {
  let service: DefaultValuePipe;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DefaultValuePipe);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
