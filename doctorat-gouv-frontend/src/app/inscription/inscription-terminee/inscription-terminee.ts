import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { InscriptionStoreService } from '../../services/inscription-store.service';
import { DemandeMiseEnRelationContextService } from '../../services/demande-mise-en-relation-context.service';

@Component({
  selector: 'app-inscription-terminee',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule],
  templateUrl: './inscription-terminee.html',
  styleUrl: './inscription-terminee.scss',
})
export class InscriptionTerminee implements OnInit {

  isDirecteur = false;

  constructor(
    private translate: TranslateService,
    private titleService: Title,
    private authService: AuthService,
    private store: InscriptionStoreService,
    private demandeContextService: DemandeMiseEnRelationContextService,
  ) { }

  ngOnInit(): void {
    this.titleService.setTitle(
      this.translate.currentLang === 'en' ? 'Registration recorded — Doctorat.gouv.fr' : 'Inscription enregistrée — Doctorat.gouv.fr'
    );
    // Le compte n'est plus auto-connecté : le rôle est lu depuis le store d'inscription
    // (avant nettoyage) puis le store est vidé pour éviter toute réutilisation.
    const coordonnees = this.store.coordonnees();
    this.isDirecteur = coordonnees?.role === 'DIRECTEUR_THESE';
    this.store.clear();
  }

  get demandeEnCours(): boolean {
    return !this.isDirecteur && this.demandeContextService.estDemandeEnCours();
  }
}
