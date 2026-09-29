import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { CandidatService } from '../../services/candidat.service';
import { DemandeCandidatContextService } from '../../services/demande-candidat-context.service';
import { DemandeCandidat } from '../../models/demande-candidat.model';

@Component({
  selector: 'app-demande-candidat',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './demande-candidat.html',
  styleUrl: './demande-candidat.scss',
})
export class DemandeCandidatComponent implements OnInit {

  demande: DemandeCandidat | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  constructor(
    private titleService: Title,
    private route: ActivatedRoute,
    private candidatService: CandidatService,
    private demandeCandidatContext: DemandeCandidatContextService,
  ) {}

  ngOnInit(): void {
    this.titleService.setTitle('Demande de mise en relation — Espace candidat — Doctorat.gouv.fr');
    // L'id ne transite pas dans l'URL : contexte en priorité, ?id= en repli (anciens liens).
    const contextId = this.demandeCandidatContext.getDemandeId();
    const idParam = this.route.snapshot.queryParamMap.get('id');
    const id = contextId ?? (idParam ? Number(idParam) : NaN);
    if (id == null || isNaN(id)) {
      this.isLoading = false;
      this.errorMessage = 'Demande introuvable.';
      return;
    }
    this.candidatService.getDemandeDetail(id).subscribe({
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
