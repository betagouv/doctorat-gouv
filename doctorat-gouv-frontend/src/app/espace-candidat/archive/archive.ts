import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { CandidatService } from '../../services/candidat.service';
import { MiseEnRelationDto, TableauDeBordCandidatItemBackend } from '../../models/mise-en-relation.model';

@Component({
  selector: 'app-archive',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './archive.html',
  styleUrl: './archive.scss',
})
export class Archive implements OnInit {

  prenom = 'Sacha';
  isLoading = false;
  errorMessage: string | null = null;

  archives: MiseEnRelationDto[] = [];

  constructor(
    private titleService: Title,
    private candidatService: CandidatService,
  ) {}

  ngOnInit(): void {
    this.titleService.setTitle('Archives — Espace candidat — Doctorat.gouv.fr');
    this.loadPrenom();
    this.loadArchives();
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

  private loadArchives(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.candidatService.getDemandesMiseEnRelation().subscribe({
      next: (items) => {
        this.isLoading = false;
        this.archives = (items ?? [])
          .filter(item => item.statut === 'ARCHIVE')
          .map(item => this.mapItem(item));
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Erreur lors du chargement des archives.';
      },
    });
  }

  get hasNoResults(): boolean {
    return !this.isLoading && !this.errorMessage && this.archives.length === 0;
  }

  private mapItem(item: TableauDeBordCandidatItemBackend): MiseEnRelationDto {
    return {
      id: item.id,
      propositionTheseId: item.propositionTheseId,
      titreSujet: item.titreSujet?.trim() ? item.titreSujet : 'Sans titre',
      etablissement: item.etablissement?.trim() ? item.etablissement : '—',
      encadrantNom: item.encadrantNom?.trim() ? item.encadrantNom : '—',
      dateContact: this.formatDate(item.dateContact),
      statut: 'archive',
    };
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
