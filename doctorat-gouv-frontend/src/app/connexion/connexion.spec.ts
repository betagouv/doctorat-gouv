import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';

import { Connexion } from './connexion';

describe('Connexion', () => {
  let component: Connexion;
  let fixture: ComponentFixture<Connexion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Connexion],
      providers: [provideRouter([]), provideHttpClient(), provideTranslateService()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Connexion);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
