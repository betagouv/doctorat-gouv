import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute} from '@angular/router';
import { PropositionTheseService } from '../services/proposition-these-service';
import { ContactContextService } from '../services/contact-context-service';
import { PropositionTheseDto } from '../models/proposition-these-dto.model';
import { DatePipe } from '@angular/common';
import { DefaultValuePipe } from '../pipes/default-value-pipe';
import { Header } from '../header/header';

@Component({
	selector: 'app-proposition-detail',
	standalone: true,
	imports: [
		CommonModule,
		RouterModule,
		DatePipe,
		DefaultValuePipe,
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
		private router: Router,
		private propositionTheseService: PropositionTheseService,
		private contactContextService: ContactContextService
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
			 // console.log('thèse : ', this.thesis);
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
	
	goToContact() {
		this.contactContextService.setContext(
		  this.thesisId,
		  this.thesis?.theseTitre ?? null,
		  this.thesis?.deposantEmail ?? null
		);

		this.router.navigate(['/contact']);

	}
	
}
