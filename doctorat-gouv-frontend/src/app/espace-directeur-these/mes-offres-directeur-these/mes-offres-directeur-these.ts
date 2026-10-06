import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { AuthService } from '../../services/auth.service';
import { DirecteurTheseService } from '../../services/directeur-these.service';
import { SujetDt } from '../../models/profil.model';

type FiltreStatut = 'tous' | 'actifs' | 'inactifs';

@Component({
  selector: 'app-mes-offres-directeur-these',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './mes-offres-directeur-these.html',
  styleUrl: './mes-offres-directeur-these.scss',
})
export class MesOffresDirecteurThese implements OnInit {

  prenom = '';
  offres: SujetDt[] = [];
  filtre: FiltreStatut = 'tous';
  isLoading = true;
  errorMessage: string | null = null;

  constructor(
    private titleService: Title,
    private authService: AuthService,
    private directeurService: DirecteurTheseService,
  ) {}

  ngOnInit(): void {
    this.titleService.setTitle('Mes offres — Espace directeur de these — Doctorat.gouv.fr');
    const user = this.authService.currentUser();
    if (user) {
      this.prenom = user.prenom;
    }
    this.loadOffres();
  }

  loadOffres(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.directeurService.getOffres().subscribe({
      next: (data) => {
        this.offres = data ?? [];
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Erreur lors du chargement de vos offres.';
      }
    });
  }

  setFiltre(filtre: FiltreStatut): void {
    this.filtre = filtre;
  }

  get offresFiltrees(): SujetDt[] {
    if (this.filtre === 'actifs') {
      return this.offres.filter(o => o.active === true);
    }
    if (this.filtre === 'inactifs') {
      return this.offres.filter(o => o.active !== true);
    }
    return this.offres;
  }

  get nbActifs(): number {
    return this.offres.filter(o => o.active === true).length;
  }

  get nbInactifs(): number {
    return this.offres.filter(o => o.active !== true).length;
  }

  isActif(offre: SujetDt): boolean {
    return offre.active === true;
  }
}
