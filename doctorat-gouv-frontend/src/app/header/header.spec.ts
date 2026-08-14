import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';

import { Header } from './header';

describe('Header', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([]), provideHttpClient(), provideTranslateService()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('affiche un bouton "Se connecter" desktop vers /connexion', () => {
    const link = fixture.nativeElement.querySelector('.fr-header__tools a.header-login-btn');
    expect(link).toBeTruthy();
    expect(link.getAttribute('href')).toBe('/connexion');
  });

  it('affiche un bouton "Se connecter" dans le menu mobile vers /connexion', () => {
    const link = fixture.nativeElement.querySelector('.mobile-menu a.mobile-login-btn');
    expect(link).toBeTruthy();
    expect(link.getAttribute('href')).toBe('/connexion');
  });
});
