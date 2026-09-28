import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { HttpClient } from '@angular/common/http';
import { DirecteurTheseService } from '../../services/directeur-these.service';
import { DemandeDtContextService } from '../../services/demande-dt-context.service';
import { DemandeDt, FichierCandidat } from '../../models/profil.model';
import { MessageEchange } from '../../models/echange.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-demande-directeur-these',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './demande-directeur-these.html',
  styleUrl: './demande-directeur-these.scss',
})
export class DemandeDirecteurThese implements OnInit, OnDestroy {

  demande: DemandeDt | null = null;
  isLoading = true;
  isAccepting = false;
  isResetting = false;
  errorMessage: string | null = null;

  messages: MessageEchange[] = [];
  messageContenu = '';
  isLoadingMessages = false;
  isSendingMessage = false;
  erreurTitre = 'Échec de l\'acceptation';
  erreurTexte = 'La mise en relation n\'a pas pu être acceptée. Veuillez réessayer.';

  private demandeId: number | null = null;
  private pollingId: ReturnType<typeof setInterval> | null = null;

  /** Bouton de réinitialisation visible uniquement hors production. */
  readonly isHorsProd = !environment.production;

  @ViewChild('dialogSucces') dialogSucces?: ElementRef<HTMLDialogElement>;
  @ViewChild('dialogErreur') dialogErreur?: ElementRef<HTMLDialogElement>;
  @ViewChild('messagesList') messagesList?: ElementRef<HTMLElement>;

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
    this.demandeId = id;
    this.directeurService.getDemandeDetail(id).subscribe({
      next: (data) => {
        this.demande = data;
        this.isLoading = false;
        if (data.statut === 'ACCEPTEE') {
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

  formatDateEnvoi(dateTime: string | null | undefined): string {
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

  accepter(): void {
    if (!this.demande || this.isAccepting || this.demande.statut !== 'CREE') {
      return;
    }
    this.isAccepting = true;
    this.directeurService.accepterDemande(this.demande.id).subscribe({
      next: (data) => {
        this.demande = data;
        this.isAccepting = false;
        this.dialogSucces?.nativeElement.showModal();
      },
      error: () => {
        this.isAccepting = false;
        this.dialogErreur?.nativeElement.showModal();
      }
    });
  }

  fermerDialogues(): void {
    this.dialogSucces?.nativeElement.close();
    this.dialogErreur?.nativeElement.close();
  }

  chargerMessages(silencieux: boolean): void {
    if (this.demandeId == null || this.demande?.statut !== 'ACCEPTEE') {
      return;
    }
    if (!silencieux) {
      this.isLoadingMessages = true;
    }
    const tailleAvant = this.messages.length;
    this.directeurService.getMessages(this.demandeId).subscribe({
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
    this.directeurService.envoyerMessage(this.demandeId, contenu).subscribe({
      next: () => {
        this.messageContenu = '';
        this.isSendingMessage = false;
        this.chargerMessages(false);
      },
      error: () => {
        this.isSendingMessage = false;
        this.erreurTitre = 'Échec de l\'envoi';
        this.erreurTexte = 'Le message n\'a pas pu être envoyé. Veuillez réessayer.';
        this.dialogErreur?.nativeElement.showModal();
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

  /** Outil hors prod : fait revenir une demande acceptée à l'état CREE. */
  reinitialiser(): void {
    if (!this.demande || this.isResetting || this.demande.statut !== 'ACCEPTEE') {
      return;
    }
    if (!confirm('Réinitialiser cette demande à l’état CREE (outil hors production) ?')) {
      return;
    }
    this.isResetting = true;
    this.directeurService.reinitialiserDemande(this.demande.id).subscribe({
      next: (data) => {
        this.demande = data;
        this.isResetting = false;
      },
      error: () => {
        this.isResetting = false;
        this.dialogErreur?.nativeElement.showModal();
      }
    });
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
