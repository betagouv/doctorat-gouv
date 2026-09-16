import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-tableau-de-bord-directeur-these',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './tableau-de-bord-directeur-these.html',
  styleUrl: './tableau-de-bord-directeur-these.scss',
})
export class TableauDeBordDirecteurThese implements OnInit {

  prenom = '';

  constructor(
    private titleService: Title,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.titleService.setTitle('Tableau de bord — Espace directeur de these — Doctorat.gouv.fr');
    const user = this.authService.currentUser();
    if (user) {
      this.prenom = user.prenom;
    }
  }
}
