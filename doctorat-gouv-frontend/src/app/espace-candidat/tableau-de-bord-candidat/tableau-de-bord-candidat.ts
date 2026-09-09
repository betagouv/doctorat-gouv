import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { MiseEnRelationDto } from '../../models/mise-en-relation.model';

@Component({
  selector: 'app-tableau-de-bord-candidat',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './tableau-de-bord-candidat.html',
  styleUrl: './tableau-de-bord-candidat.scss',
})
export class TableauDeBordCandidat implements OnInit {

  prenom = 'Sacha';
  activeFilter: 'en_cours' | 'archivees' = 'en_cours';

  brouillons: MiseEnRelationDto[] = [];
  enAttente: MiseEnRelationDto[] = [];
  misesEnRelation: MiseEnRelationDto[] = [];

  constructor(private titleService: Title) {}

  ngOnInit(): void {
    this.titleService.setTitle('Tableau de bord — Espace candidat — Doctorat.gouv.fr');
    this.loadData();
  }

  setFilter(filter: 'en_cours' | 'archivees'): void {
    this.activeFilter = filter;
  }

  private loadData(): void {
    this.brouillons = [
      {
        id: 1,
        titreSujet: 'Impacts des extrêmes climatiques sur le secteur de l\'énergie : vers l\'identification d...',
        etablissement: 'Université de Bretagne Occidentale',
        encadrantNom: 'Laurent Chauvaud',
        dateContact: '24 juin 2026',
        statut: 'brouillon',
      },
    ];

    this.enAttente = [
      {
        id: 2,
        titreSujet: 'Impacts des extrêmes climatiques sur le secteur de l\'énergie : vers l\'identification d...',
        etablissement: 'Université de Bretagne Occidentale',
        encadrantNom: 'Laurent Chauvaud',
        dateContact: '24 juin 2026',
        statut: 'en_attente',
      },
      {
        id: 3,
        titreSujet: 'Instabilités fluide-élastiques de surfaces de kirigami',
        etablissement: 'Institut Polytechnique de Paris École...',
        encadrantNom: 'Laurent Chauvaud',
        dateContact: '24 juin 2026',
        statut: 'en_attente',
      },
    ];

    this.misesEnRelation = [
      {
        id: 4,
        titreSujet: 'Etude des représentations latentes des LLMs seuls ou en architecture multi-agents au tra...',
        etablissement: 'IMT Nord Europe',
        encadrantNom: 'Sophie Dupuis',
        dateContact: '24 juin 2026',
        statut: 'mise_en_relation',
      },
      {
        id: 5,
        titreSujet: 'Résolution haute performance d\'équations différentielles linéaires à précisions multiples',
        etablissement: 'IMT Nord Europe',
        encadrantNom: 'Sophie Dupuis',
        dateContact: '24/06/2026',
        statut: 'mise_en_relation',
        nbMessages: 1,
      },
      {
        id: 6,
        titreSujet: 'Instabilités fluide-élastiques de surfaces de kirigami',
        etablissement: 'IMT Nord Europe',
        encadrantNom: 'Sophie Dupuis',
        dateContact: '24 juin 2026',
        statut: 'mise_en_relation',
        candidatureEnvoyee: true,
      },
    ];
  }
}
