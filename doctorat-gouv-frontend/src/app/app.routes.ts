import { Routes } from '@angular/router';
import { Search } from './search/search';
import { PropositionDetail } from './proposition-detail/proposition-detail';
import { Contact } from './contact/contact';
import { Connexion } from './connexion/connexion';
import { InscriptionCoordonnees } from './inscription/inscription-coordonnees/inscription-coordonnees';
import { InscriptionDocuments } from './inscription/inscription-documents/inscription-documents';
import { InscriptionTerminee } from './inscription/inscription-terminee/inscription-terminee';
import { inscriptionGuard } from './inscription/inscription.guard';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: Search },
  { path: 'search', component: Search },
  { path: 'proposition', component: PropositionDetail },
  { path: 'contact', component: Contact },
  { path: 'connexion', component: Connexion },
  { path: 'inscription', redirectTo: 'inscription/coordonnees', pathMatch: 'full' },
  { path: 'inscription/coordonnees', component: InscriptionCoordonnees },
  { path: 'inscription/documents', component: InscriptionDocuments, canActivate: [inscriptionGuard] },
  { path: 'inscription/terminee', component: InscriptionTerminee, canActivate: [authGuard] },
];
