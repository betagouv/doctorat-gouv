import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { PropositionTheseService } from '../services/proposition-these-service';
import { PropositionTheseDto } from '../models/proposition-these-dto.model';
import { DatePipe } from '@angular/common';

import { Header } from '../header/header';

@Component({
	selector: 'app-proposition-detail',
	standalone: true,
	imports: [
		CommonModule,
		RouterModule,
		DatePipe,
		Header
	],
	templateUrl: './proposition-detail.html',
	styleUrl: './proposition-detail.scss',
})
export class PropositionDetail {

	thesisId!: number;
	thesis: PropositionTheseDto | null = null;

	constructor(
		private route: ActivatedRoute,
		private propositionTheseService: PropositionTheseService
	) { }


	ngOnInit(): void {
		 // 1️ - Lecture du paramètre d'URL (toujours une chaîne)
		 const idParam = this.route.snapshot.queryParamMap.get('id');
		 // 2️ - Conversion en nombre – si la conversion échoue, on obtient NaN
		 this.thesisId = idParam ? Number(idParam) : NaN;

		 // 3️ - Appel du service
		 this.propositionTheseService
		   .getThesisById(this.thesisId)               // le service attend un number
		   .subscribe((data: PropositionTheseDto) => {
		     this.thesis = data;
			 console.log('thèse55 : ', this.thesis);

			
		   });
	}
	
	getFirstDomaine(thesis: { domainesImpactListe: string[] | null }): string | null {
	  return thesis.domainesImpactListe?.[0] ?? null;
	}
	
	getFirstDomaineWithMaxLength(
	  thesis: { domainesImpactListe: string[] | null },
	  maxLength = 10
	): string | null {
	  const domaine = thesis.domainesImpactListe?.[0];
	  return domaine
	    ? domaine.length > maxLength
	      ? domaine.slice(0, maxLength) + '…'
	      : domaine
	    : null;
	}

}
