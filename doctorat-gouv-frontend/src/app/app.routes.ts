import { Routes } from '@angular/router';
import { Search } from './search/search';
import { PropositionDetail } from './proposition-detail/proposition-detail';
import { Contact } from './contact/contact';
import { Connexion } from './connexion/connexion';
import { Inscription } from './inscription/inscription';

export const routes: Routes = [
  { path: '', component: Search },
  { path: 'search', component: Search },
  { path: 'proposition', component: PropositionDetail },
  { path: 'contact', component: Contact },
  { path: 'connexion', component: Connexion },
  { path: 'inscription', component: Inscription }
];
