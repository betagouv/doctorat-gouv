import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { AuthService } from '../../services/auth.service';
import { DirecteurTheseService } from '../../services/directeur-these.service';
import { SujetDt } from '../../models/profil.model';

type FiltreStatut = 'tous' | 'actifs' | 'inactifs';

@Component({
  selector: 'app-mes-sujets-directeur-these',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './mes-sujets-directeur-these.html',
  styleUrl: './mes-sujets-directeur-these.scss',
})
export class MesSujetsDirecteurThese implements OnInit {

  prenom = '';
  sujets: SujetDt[] = [];
  filtre: FiltreStatut = 'tous';
  isLoading = true;
  errorMessage: string | null = null;

  constructor(
    private titleService: Title,
    private authService: AuthService,
    private directeurService: DirecteurTheseService,
  ) {}

  ngOnInit(): void {
    this.titleService.setTitle('Mes sujets — Espace directeur de these — Doctorat.gouv.fr');
    const user = this.authService.currentUser();
    if (user) {
      this.prenom = user.prenom;
    }
    this.loadSujets();
  }

  loadSujets(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.directeurService.getSujets().subscribe({
      next: (data) => {
        this.sujets = data ?? [];
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Erreur lors du chargement de vos sujets.';
      }
    });
  }

  setFiltre(filtre: FiltreStatut): void {
    this.filtre = filtre;
  }

  get sujetsFiltres(): SujetDt[] {
    if (this.filtre === 'actifs') {
      return this.sujets.filter(s => s.active === true);
    }
    if (this.filtre === 'inactifs') {
      return this.sujets.filter(s => s.active !== true);
    }
    return this.sujets;
  }

  get nbActifs(): number {
    return this.sujets.filter(s => s.active === true).length;
  }

  get nbInactifs(): number {
    return this.sujets.filter(s => s.active !== true).length;
  }

  isActif(sujet: SujetDt): boolean {
    return sujet.active === true;
  }
}
