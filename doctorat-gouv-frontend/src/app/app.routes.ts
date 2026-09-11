import { Routes } from '@angular/router';
import { Search } from './search/search';
import { PropositionDetail } from './proposition-detail/proposition-detail';
import { Contact } from './contact/contact';
import { Connexion } from './connexion/connexion';
import { InscriptionCoordonnees } from './inscription/inscription-coordonnees/inscription-coordonnees';
import { InscriptionDocuments } from './inscription/inscription-documents/inscription-documents';
import { InscriptionTerminee } from './inscription/inscription-terminee/inscription-terminee';
import { EspaceCandidat } from './espace-candidat/espace-candidat';
import { TableauDeBordCandidat } from './espace-candidat/tableau-de-bord-candidat/tableau-de-bord-candidat';
import { Archive } from './espace-candidat/archive/archive';
import { DemandeMiseEnRelation } from './demande-mise-en-relation/demande-mise-en-relation';
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
  { path: 'espace-candidat', component: EspaceCandidat, canActivate: [authGuard] },
  { path: 'tableau-de-bord-candidat', component: TableauDeBordCandidat, canActivate: [authGuard] },
  { path: 'archive', component: Archive, canActivate: [authGuard] },
  { path: 'demande-mise-en-relation', component: DemandeMiseEnRelation, canActivate: [authGuard] },
];
