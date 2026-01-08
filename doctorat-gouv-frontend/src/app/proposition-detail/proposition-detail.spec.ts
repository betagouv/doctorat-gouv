import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PropositionDetail } from './proposition-detail';

describe('PropositionDetail', () => {
  let component: PropositionDetail;
  let fixture: ComponentFixture<PropositionDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PropositionDetail]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PropositionDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
