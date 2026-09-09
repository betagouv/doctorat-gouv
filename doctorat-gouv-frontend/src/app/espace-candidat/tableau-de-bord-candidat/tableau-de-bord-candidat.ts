import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-tableau-de-bord-candidat',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tableau-de-bord-candidat.html',
  styleUrl: './tableau-de-bord-candidat.scss',
})
export class TableauDeBordCandidat implements OnInit {

  constructor(private titleService: Title) {}

  ngOnInit(): void {
    this.titleService.setTitle('Tableau de bord — Espace candidat — Doctorat.gouv.fr');
  }
}
