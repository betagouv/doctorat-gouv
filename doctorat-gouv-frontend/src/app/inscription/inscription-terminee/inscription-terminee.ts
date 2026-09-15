import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-inscription-terminee',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule],
  templateUrl: './inscription-terminee.html',
  styleUrl: './inscription-terminee.scss',
})
export class InscriptionTerminee implements OnInit {

  isDirecteur = false;

  constructor(
    private translate: TranslateService,
    private titleService: Title,
    private authService: AuthService,
  ) { }

  ngOnInit(): void {
    this.titleService.setTitle(
      this.translate.currentLang === 'en' ? 'Registration confirmed — Doctorat.gouv.fr' : 'Inscription validée — Doctorat.gouv.fr'
    );
    const user = this.authService.currentUser();
    this.isDirecteur = user?.role === 'DIRECTEUR_THESE';
  }
}
