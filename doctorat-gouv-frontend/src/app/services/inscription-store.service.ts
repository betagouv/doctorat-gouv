import { Injectable, signal } from '@angular/core';

export type Demarche = 'DOCTORAT' | 'PARTENARIAT';

export interface InscriptionCoordonnees {
  demarche: Demarche;
  nom: string;
  prenom: string;
  civilite?: string;
  situation?: string;
  email: string;
  telephone?: string;
  masterConfirme: boolean;
}

@Injectable({ providedIn: 'root' })
export class InscriptionStoreService {

  private readonly coordonneesSignal = signal<InscriptionCoordonnees | null>(null);

  readonly coordonnees = this.coordonneesSignal.asReadonly();

  setCoordonnees(data: InscriptionCoordonnees): void {
    this.coordonneesSignal.set(data);
  }

  clear(): void {
    this.coordonneesSignal.set(null);
  }
}
