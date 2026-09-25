import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { AuthService } from '../../services/auth.service';
import { DemandeDtContextService } from '../../services/demande-dt-context.service';
import { DirecteurTheseService } from '../../services/directeur-these.service';
import { DemandeDt } from '../../models/profil.model';

@Component({
  selector: 'app-tableau-de-bord-directeur-these',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './tableau-de-bord-directeur-these.html',
  styleUrl: './tableau-de-bord-directeur-these.scss',
})
export class TableauDeBordDirecteurThese implements OnInit {

  prenom = '';
  demandes: DemandeDt[] = [];
  isLoadingDemandes = true;
  errorDemandes: string | null = null;

  constructor(
    private titleService: Title,
    private authService: AuthService,
    private directeurService: DirecteurTheseService,
    private demandeDtContext: DemandeDtContextService,
  ) {}

  ngOnInit(): void {
    this.titleService.setTitle('Tableau de bord — Espace directeur de these — Doctorat.gouv.fr');
    const user = this.authService.currentUser();
    if (user) {
      this.prenom = user.prenom;
    }
    this.loadDemandes();
  }

  loadDemandes(): void {
    this.isLoadingDemandes = true;
    this.errorDemandes = null;
    this.directeurService.getDemandes().subscribe({
      next: (data) => {
        this.demandes = data ?? [];
        this.isLoadingDemandes = false;
      },
      error: () => {
        this.isLoadingDemandes = false;
        this.errorDemandes = 'Erreur lors du chargement des demandes de mise en relation.';
      }
    });
  }

  /** Mémorise la demande sélectionnée (sans l'exposer dans l'URL). */
  selectDemande(id: number): void {
    this.demandeDtContext.setDemandeId(id);
  }

  /** Colonne EN ATTENTE : demandes envoyées, en attente de décision du DT. */
  get enAttente(): DemandeDt[] {
    return this.demandes.filter(d => d.statut === 'CREE');
  }

  /** Colonne MISES EN RELATION : demandes acceptées par le DT (fonctionnalité à venir). */
  get misesEnRelation(): DemandeDt[] {
    return this.demandes.filter(d => d.statut === 'ACCEPTEE');
  }

  initiales(nom: string | null): string {
    if (!nom) {
      return '?';
    }
    return nom.trim().split(/\s+/).map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase();
  }

  formatDateCandidature(dateTime: string | null): string {
    if (!dateTime) {
      return '—';
    }
    const date = new Date(dateTime.replace(' ', 'T'));
    if (isNaN(date.getTime())) {
      return dateTime;
    }
    const jour = date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
    const heure = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }).replace(':', 'h');
    return `${jour} à ${heure}`;
  }
}
