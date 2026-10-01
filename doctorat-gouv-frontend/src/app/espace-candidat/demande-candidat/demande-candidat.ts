import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { CandidatService } from '../../services/candidat.service';
import { DemandeCandidatContextService } from '../../services/demande-candidat-context.service';
import { DemandeCandidat } from '../../models/demande-candidat.model';
import { MessageEchange } from '../../models/echange.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-demande-candidat',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './demande-candidat.html',
  styleUrl: './demande-candidat.scss',
})
export class DemandeCandidatComponent implements OnInit, OnDestroy {

  demande: DemandeCandidat | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  messages: MessageEchange[] = [];
  messageContenu = '';
  isLoadingMessages = false;
  isSendingMessage = false;
  isDesisting = false;
  messageErreur: string | null = null;

  motifDesistement = '';
  isSendingDesistement = false;
  isAnnulationDesistement = false;

  /** Bouton d'annulation visible uniquement hors production. */
  readonly isHorsProd = !environment.production;

  private demandeId: number | null = null;
  private pollingId: ReturnType<typeof setInterval> | null = null;

  @ViewChild('messagesList') messagesList?: ElementRef<HTMLElement>;
  @ViewChild('dialogDesistement') dialogDesistement?: ElementRef<HTMLDialogElement>;

  constructor(
    private titleService: Title,
    private router: Router,
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
    this.demandeId = id;
    this.candidatService.getDemandeDetail(id).subscribe({
      next: (data) => {
        this.demande = data;
        this.isLoading = false;
        if (data.statut === 'ACCEPTEE' && !data.archivee) {
          this.chargerMessages(false);
        }
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Demande introuvable.';
      }
    });
    this.pollingId = setInterval(() => this.chargerMessages(true), 15000);
  }

  ngOnDestroy(): void {
    if (this.pollingId != null) {
      clearInterval(this.pollingId);
      this.pollingId = null;
    }
  }

  initiales(nom: string | null): string {
    if (!nom) {
      return '?';
    }
    return nom.trim().split(/\s+/).map(p => p.charAt(0)).join('').slice(0, 2).toUpperCase();
  }

  get directeurNomComplet(): string {
    if (!this.demande) {
      return '';
    }
    return `${this.demande.directeurPrenom || ''} ${this.demande.directeurNom || ''}`.trim();
  }

  formatDatePublie(dateTime: string | null): string {
    if (!dateTime) {
      return '—';
    }
    const date = new Date(dateTime.replace(' ', 'T'));
    if (isNaN(date.getTime())) {
      return dateTime;
    }
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  formatDateLimite(dateTime: string | null): string {
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

  formatDateDebut(dateStr: string | null): string {
    if (!dateStr) {
      return '—';
    }
    const parts = dateStr.split('-');
    if (parts.length !== 3) {
      return dateStr;
    }
    const date = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
    if (isNaN(date.getTime())) {
      return dateStr;
    }
    const formatted = date.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }

  chargerMessages(silencieux: boolean): void {
    if (this.demandeId == null || this.demande?.statut !== 'ACCEPTEE' || this.demande?.archivee) {
      return;
    }
    if (!silencieux) {
      this.isLoadingMessages = true;
    }
    const tailleAvant = this.messages.length;
    this.candidatService.getMessages(this.demandeId).subscribe({
      next: (data) => {
        this.messages = data ?? [];
        this.isLoadingMessages = false;
        if (this.messages.length > tailleAvant) {
          this.scrollerMessagesBas();
        }
      },
      error: () => {
        this.isLoadingMessages = false;
      }
    });
  }

  envoyerMessage(): void {
    if (this.demandeId == null || this.isSendingMessage) {
      return;
    }
    const contenu = this.messageContenu.trim();
    if (!contenu) {
      return;
    }
    this.isSendingMessage = true;
    this.messageErreur = null;
    this.candidatService.envoyerMessage(this.demandeId, contenu).subscribe({
      next: () => {
        this.messageContenu = '';
        this.isSendingMessage = false;
        this.chargerMessages(false);
      },
      error: () => {
        this.isSendingMessage = false;
        this.messageErreur = 'Le message n\'a pas pu être envoyé. Veuillez réessayer.';
      }
    });
  }

  /** Bouchon : fonctionnalité « Je ne suis plus intéressé » à détailler (≠ archivage). */
  seDesister(): void {
    if (!this.demande || this.demande.archivee || this.demande.statut !== 'ACCEPTEE') {
      return;
    }
    this.motifDesistement = '';
    this.dialogDesistement?.nativeElement.showModal();
  }

  fermerDialogueDesistement(): void {
    this.dialogDesistement?.nativeElement.close();
  }

  envoyerDesistement(): void {
    if (!this.demande || this.isSendingDesistement) {
      return;
    }
    const motif = this.motifDesistement.trim();
    if (!motif) {
      return;
    }
    this.isSendingDesistement = true;
    this.messageErreur = null;
    this.candidatService.seDesister(this.demande.id, motif).subscribe({
      next: () => {
        this.isSendingDesistement = false;
        this.fermerDialogueDesistement();
        this.router.navigate(['/tableau-de-bord-candidat']);
      },
      error: () => {
        this.isSendingDesistement = false;
        this.messageErreur = 'Le désistement n\'a pas pu être enregistré. Veuillez réessayer.';
      }
    });
  }

  /** Outil hors prod : annule un désistement et revient à l'état accepté. */
  annulerDesistement(): void {
    if (!this.demande || this.isAnnulationDesistement) {
      return;
    }
    if (!confirm('Annuler ce désistement et revenir à l’état accepté (outil hors production) ?')) {
      return;
    }
    this.isAnnulationDesistement = true;
    this.candidatService.annulerDesistement(this.demande.id).subscribe({
      next: (data) => {
        this.demande = data;
        this.isAnnulationDesistement = false;
        if (data.statut === 'ACCEPTEE' && !data.archivee) {
          this.chargerMessages(false);
        }
      },
      error: () => {
        this.isAnnulationDesistement = false;
        this.messageErreur = 'L\'annulation n\'a pas pu être effectuée. Veuillez réessayer.';
      }
    });
  }

  private scrollerMessagesBas(): void {
    setTimeout(() => {
      const el = this.messagesList?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }
}
