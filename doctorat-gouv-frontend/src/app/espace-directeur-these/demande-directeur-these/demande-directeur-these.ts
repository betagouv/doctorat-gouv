import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { HttpClient } from '@angular/common/http';
import { DirecteurTheseService } from '../../services/directeur-these.service';
import { DemandeDtContextService } from '../../services/demande-dt-context.service';
import { DemandeDt, FichierCandidat } from '../../models/profil.model';
import { environment } from '../../../environments/environment';

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

  private readonly apiUrl = `${environment.apiUrl}/directeur-these`;

  constructor(
    private titleService: Title,
    private route: ActivatedRoute,
    private directeurService: DirecteurTheseService,
    private demandeDtContext: DemandeDtContextService,
    private http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.titleService.setTitle('Demande de mise en relation — Espace directeur de these — Doctorat.gouv.fr');
    // L'id ne transite plus dans l'URL : contexte en priorité, ?id= en repli (anciens liens).
    const contextId = this.demandeDtContext.getDemandeId();
    const idParam = this.route.snapshot.queryParamMap.get('id');
    const id = contextId ?? (idParam ? Number(idParam) : NaN);
    if (id == null || isNaN(id)) {
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

  initiales(nom: string | null): string {
    if (!nom) {
      return '?';
    }
    return nom.trim().split(/\s+/).map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase();
  }

  get civiliteNom(): string {
    if (!this.demande) {
      return '';
    }
    const civilite = this.demande.candidatCivilite ? `${this.demande.candidatCivilite} ` : '';
    return `${civilite}${this.demande.candidatNom || ''}`.trim();
  }

  formatDatePublie(dateTime: string | null | undefined): string {
    if (!dateTime) {
      return '—';
    }
    const date = new Date(dateTime.replace(' ', 'T'));
    if (isNaN(date.getTime())) {
      return dateTime;
    }
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  formatDateLimite(dateTime: string | null | undefined): string {
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

  formatDateEnvoi(dateTime: string | null): string {
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

  formatTaille(bytes: number | null | undefined): string {
    if (bytes == null) {
      return 'PDF';
    }
    const ko = bytes / 1024;
    return `PDF – ${ko.toFixed(2).replace('.', ',')} Ko`;
  }

  nomAffichage(filename: string | null | undefined): string {
    if (!filename) {
      return '';
    }
    return filename.replace(/\.pdf$/i, '');
  }

  /** Bouchon : simule l'envoi de l'e-mail de notification au candidat (non développé). */
  simulateurEnvoiNotification(): void {
    // Envoi d'e-mail de notification non développé pour l'instant.
  }

  ouvrirFichier(type: 'cv' | 'piece', index?: number): void {
    if (!this.demande) {
      return;
    }
    const body: { demandeId: number; type: string; index?: number } = {
      demandeId: this.demande.id,
      type,
    };
    if (type === 'piece' && index != null) {
      body.index = index;
    }
    const url = `${this.apiUrl}/mises-en-relation/fichier`;
    this.http.post(url, body, { responseType: 'blob', observe: 'response' }).subscribe({
      next: (response) => {
        const blob = new Blob([response.body as Blob], { type: 'application/pdf' });
        const objectUrl = URL.createObjectURL(blob);
        window.open(objectUrl, '_blank');
        setTimeout(() => URL.revokeObjectURL(objectUrl), 60000);
      },
      error: () => {
        // Fichier indisponible
      }
    });
  }
}
