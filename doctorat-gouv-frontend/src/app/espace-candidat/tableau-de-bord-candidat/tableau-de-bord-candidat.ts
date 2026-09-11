import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { CandidatService } from '../../services/candidat.service';
import { MiseEnRelationDto, TableauDeBordCandidatItemBackend } from '../../models/mise-en-relation.model';

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
  isLoading = false;
  errorMessage: string | null = null;

  brouillons: MiseEnRelationDto[] = [];
  enAttente: MiseEnRelationDto[] = [];
  misesEnRelation: MiseEnRelationDto[] = [];
  archives: MiseEnRelationDto[] = [];

  private allItems: TableauDeBordCandidatItemBackend[] = [];

  constructor(
    private titleService: Title,
    private candidatService: CandidatService,
  ) {}

  ngOnInit(): void {
    this.titleService.setTitle('Tableau de bord — Espace candidat — Doctorat.gouv.fr');
    this.loadPrenom();
    this.loadDemandes();
  }

  setFilter(filter: 'en_cours' | 'archivees'): void {
    this.activeFilter = filter;
  }

  private loadPrenom(): void {
    this.candidatService.getProfil().subscribe({
      next: (profil) => {
        if (profil.prenom) {
          this.prenom = profil.prenom;
        }
      },
    });
  }

  private loadDemandes(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.candidatService.getDemandesMiseEnRelation().subscribe({
      next: (items) => {
        this.isLoading = false;
        this.repartirDemandes(items ?? []);
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Erreur lors du chargement du tableau de bord.';
      },
    });
  }

  private repartirDemandes(items: TableauDeBordCandidatItemBackend[]): void {
    this.allItems = items;
    this.reorganiser();
  }

  private reorganiser(): void {
    this.brouillons = [];
    this.enAttente = [];
    this.misesEnRelation = [];
    this.archives = [];

    for (const item of this.allItems) {
      const mapped: MiseEnRelationDto = {
        id: item.id,
        propositionTheseId: item.propositionTheseId,
        titreSujet: item.titreSujet?.trim() ? item.titreSujet : 'Sans titre',
        etablissement: item.etablissement?.trim() ? item.etablissement : '—',
        encadrantNom: item.encadrantNom?.trim() ? item.encadrantNom : '—',
        dateContact: this.formatDate(item.dateContact),
        statut: item.statut === 'BROUILLON' ? 'brouillon' : item.statut === 'ARCHIVE' ? 'archive' : 'en_attente',
      };
      if (item.statut === 'BROUILLON') {
        this.brouillons.push(mapped);
      } else if (item.statut === 'ARCHIVE') {
        this.archives.push(mapped);
      } else {
        this.enAttente.push(mapped);
      }
    }
  }

  get hasNoResults(): boolean {
    if (this.activeFilter === 'archivees') {
      return !this.isLoading && !this.errorMessage && this.archives.length === 0;
    }
    return !this.isLoading && !this.errorMessage && this.brouillons.length === 0 && this.enAttente.length === 0 && this.misesEnRelation.length === 0;
  }

  private formatDate(isoDate: string | null): string {
    if (!isoDate) {
      return '—';
    }
    const date = new Date(isoDate);
    if (isNaN(date.getTime())) {
      return isoDate;
    }
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }
}
