import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute} from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PropositionTheseService } from '../services/proposition-these-service';
import { ContactContextService } from '../services/contact-context-service';
import { PropositionTheseDto } from '../models/proposition-these-dto.model';
import { DynamicDatePipe } from '../pipes/dynamic-date-pipe';
import { DefaultValuePipe } from '../pipes/default-value-pipe';
import { TranslateService } from '@ngx-translate/core';
import { Header } from '../header/header';

@Component({
	selector: 'app-proposition-detail',
	standalone: true,
	imports: [
		CommonModule,
		RouterModule,
		DefaultValuePipe,
		TranslateModule,
		DynamicDatePipe,
		Header
	],
	templateUrl: './proposition-detail.html',
	styleUrl: './proposition-detail.scss',
})
export class PropositionDetail {

	thesisId!: number;
	thesis: PropositionTheseDto | null = null;
	
	errorMessage: string | null = null;
	
	defisSocieteTranslations: Record<string, string> = {
	    // Défis de société
	    "Santé": "Health",
	    "Culture, créativité, société": "Culture, creativity, society",
	    "Sécurité civile pour la société": "Civil security for society",
	    "Numérique, industrie, espace": "Digital, industry, space",
	    "Climat, énergie, mobilité": "Climate, energy, mobility",
	    "Alimentation, bioéconomie, ressources naturelles, agriculture et environnement":
	      "Food, bioeconomy, natural resources, agriculture and environment",

	    // ODD (ONU)
	    "Pas de pauvreté": "No Poverty",
	    "Faim \"zéro\"": "Zero Hunger",
	    "Bonne santé et bien-être": "Good Health and Well-being",
	    "Éducation de qualité": "Quality Education",
	    "Égalité entre les sexes": "Gender Equality",
	    "Eau propre et assainissement": "Clean Water and Sanitation",
	    "Énergie propre et d'un coût abordable": "Affordable and Clean Energy",
	    "Travail décent et croissance économique": "Decent Work and Economic Growth",
	    "Industrie, innovation et infrastructure": "Industry, Innovation and Infrastructure",
	    "Inégalités réduites": "Reduced Inequalities",
	    "Villes et communautés durables": "Sustainable Cities and Communities",
	    "Consommation et production responsables": "Responsible Consumption and Production",
	    "Mesures relatives à la lutte contre les changements climatiques": "Climate Action",
	    "Vie aquatique": "Life Below Water",
	    "Vie terrestre": "Life on Land",
	    "Paix, justice et institutions efficaces": "Peace, Justice and Strong Institutions",
	    "Partenariats pour la réalisation des objectifs": "Partnerships for the Goals"
	  };

	constructor(
		private route: ActivatedRoute,
		private router: Router,
		private propositionTheseService: PropositionTheseService,
		private contactContextService: ContactContextService,
		public translate: TranslateService
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
	
	getFirstDomaineWithMaxLength(thesis: { domainesImpactListe: string[] | null }, maxLength = 10): string | null {
		const domaine = thesis.domainesImpactListe?.[0];
		if (!domaine) return null;

		// 🔥 Traduction FR → EN si nécessaire
		const label = this.getDomaineLabel(domaine);

		// 🔥 Coupe propre
		return label.length > maxLength ? label.slice(0, maxLength) + '…' : label;
	}
	
	getDomaineLabel(domaine: string): string {
	  if (this.translate.currentLang === 'en') {
	    return this.defisSocieteTranslations[domaine] || domaine;
	  }
	  return domaine;
	}
	
	goToContact() {
	  // 1 - Vérification stricte du sujet
	  if (!this.thesis || !this.thesisId || this.thesisId === 0) {
	    console.warn("Sujet invalide : impossible d’ouvrir le formulaire de contact.");
	    this.errorMessage = "Ce sujet n’est pas disponible pour une prise de contact.";
	    return;
	  }

	  // 2 - Mise à jour du contexte (sessionStorage + mémoire)
	  this.contactContextService.setContext(
	    this.thesisId,
	    this.thesis.theseTitre ?? null,
	    this.thesis.deposantEmail ?? null,
	    this.thesis.typeProposition ?? null
	  );

	  // 3 - Navigation vers la page contact
	  this.router.navigate(['/contact']);
	}
	
	getThesisTitle(thesis: any): string {
	  const lang = this.translate.currentLang;

	  if (lang === 'en') {
	    if (thesis.theseTitreAnglais && thesis.theseTitreAnglais.trim() !== '') {
	      return thesis.theseTitreAnglais;
	    }
	  }

	  return thesis.theseTitre;
	}
	
	getThesisResume(thesis: any): string {
	  const lang = this.translate.currentLang;

	  if (lang === 'en') {
	    if (thesis.resumeAnglais && thesis.resumeAnglais.trim() !== '') {
	      return thesis.resumeAnglais;
	    }
	  }

	  return thesis.resume;
	}
	
	getProfilRecherche(thesis: any): string {
	  const lang = this.translate.currentLang;

	  if (lang === 'en') {
	    if (thesis.profilRechercheAnglais && thesis.profilRechercheAnglais.trim() !== '') {
	      return thesis.profilRechercheAnglais;
	    }
	  }

	  return thesis.profilRecherche;
	}
	
	getMotsCles(thesis: any): Record<string, string> {
	  const lang = this.translate.currentLang;

	  const fr = thesis?.motsCles || {};
	  const en = thesis?.motsClesAnglais || {};

	  const result: Record<string, string> = {};

	  // On part des clés FR (référence)
	  Object.keys(fr).forEach(key => {
	    const frValue = fr[key];
	    const enValue = en[key];

	    if (lang === 'en') {
	      if (enValue && enValue.trim() !== '') {
	        result[key] = enValue;
	        return;
	      }
	    }

	    // Fallback sur FR
	    result[key] = frValue;
	  });

	  return result;
	}


	get currentLang(): string {
	  return this.translate.currentLang;
	}

}
