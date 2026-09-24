import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { DirecteurTheseService } from '../../services/directeur-these.service';
import { DemandeDt } from '../../models/profil.model';

@Component({
  selector: 'app-demande-directeur-these',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './demande-directeur-these.html',
  styleUrl: './demande-directeur-these.scss',
})
export class DemandeDirecteurThese implements OnInit {

  demande: DemandeDt | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  constructor(
    private titleService: Title,
    private route: ActivatedRoute,
    private directeurService: DirecteurTheseService,
  ) {}

  ngOnInit(): void {
    this.titleService.setTitle('Demande de mise en relation — Espace directeur de these — Doctorat.gouv.fr');
    const idParam = this.route.snapshot.queryParamMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (!idParam || isNaN(id)) {
      this.isLoading = false;
      this.errorMessage = 'Demande introuvable.';
      return;
    }
    this.directeurService.getDemandeDetail(id).subscribe({
      next: (data) => {
        this.demande = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Demande introuvable.';
      }
    });
  }
}
