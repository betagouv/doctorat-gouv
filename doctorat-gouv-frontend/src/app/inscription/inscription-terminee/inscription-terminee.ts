import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

/**
 * Page de confirmation finale de l'inscription (Étape 3).
 * Affiche un message de succès et les prochaines étapes, l'utilisateur étant
 * déjà connecté (le token a été posé côté frontend lors du dernier appel).
 */
@Component({
  selector: 'app-inscription-terminee',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule],
  templateUrl: './inscription-terminee.html',
  styleUrl: './inscription-terminee.scss',
})
export class InscriptionTerminee implements OnInit {

  constructor(
    private translate: TranslateService,
    private titleService: Title,
  ) { }

  ngOnInit(): void {
    this.titleService.setTitle(
      this.translate.currentLang === 'en' ? 'Registration confirmed — Doctorat.gouv.fr' : 'Inscription validée — Doctorat.gouv.fr'
    );
  }
}
