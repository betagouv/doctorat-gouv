import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Title } from '@angular/platform-browser';

/**
 * Page tableau de bord « Espace candidat » (protégée par authGuard).
 * Pour l'instant volontairement vide (placeholder) ; à garnir ultérieurement
 * (candidatures, profil, pièces, etc.).
 */
@Component({
  selector: 'app-espace-candidat',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './espace-candidat.html',
  styleUrl: './espace-candidat.scss',
})
export class EspaceCandidat implements OnInit {

  constructor(
    private translate: TranslateService,
    private titleService: Title,
  ) { }

  ngOnInit(): void {
    this.titleService.setTitle(
      this.translate.currentLang === 'en' ? 'Candidate space — Doctorat.gouv.fr' : 'Espace candidat — Doctorat.gouv.fr'
    );
  }
}
