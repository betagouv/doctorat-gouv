import { Routes } from '@angular/router';
import { Home } from './home/home';
import { Search } from './search/search';
import { PropositionDetail } from './proposition-detail/proposition-detail';
import { Contact } from './contact/contact';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'home', component: Home },
  { path: 'search', component: Search },
  { path: 'proposition', component: PropositionDetail },
  { path: 'contact', component: Contact }
];
