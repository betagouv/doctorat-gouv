package fr.dinum.beta.gouv.doctorat.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.dinum.beta.gouv.doctorat.config.EuraxessProperties;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.ApplicationDetails;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.Description;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.DisciplineEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.HowToApplyEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.JobOpportunity;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.LanguagesEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.LanguagesLevelEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.RequiredLanguages;

/**
 * Tests unitaires du mapper EURAXESS : mapping champ à champ, replis R3,
 * exclusions R4, troncatures R5, omission des langues (R7).
 */
class EuraxessFeedMapperTest {

	private EuraxessProperties properties;
	private EuraxessFeedMapper mapper;

	@BeforeEach
	void setUp() {
		properties = new EuraxessProperties();
		properties.setOrganisationIdKey("doctorat-gouv-dev");
		properties.setDatasourceKey("doctorat-gouv");
		properties.setContactEmail("contact@doctorat.gouv.fr");
		mapper = new EuraxessFeedMapper(properties);
	}

	private PropositionThese completeProposition() {
		PropositionThese p = new PropositionThese();
		p.setMatricule("AB-2026_0001");
		p.setTheseTitre("Titre de la thèse");
		p.setTheseTitreAnglais("Thesis title");
		p.setResume("Résumé court");
		p.setResumeAnglais("Short summary");
		p.setObjectif("Objectif");
		p.setDateMaj(LocalDateTime.of(2026, 8, 1, 12, 30, 0));
		p.setDateLimiteCandidature(LocalDateTime.of(2026, 10, 1, 0, 0, 0));
		p.setDomaineScientifique("1");
		p.setSpecialite("physique");
		p.setEtablissementLibelle("Université Exemple");
		p.setEtablissementVille("Paris");
		p.setEtablissementCodePostal("75000");
		p.setUniteRechercheLibelle("Laboratoire de Recherche");
		p.setEcoleDoctoraleLibelle("École Doctorale Exemple");
		p.setDeposantEmail("deposant@exemple.fr");
		p.setDirectionTheseEmail("directeur@exemple.fr");
		p.setNiveauAnglaisRequis("C1");
		p.setNiveauFrancaisRequis("C2");
		p.setUrlCandidature("https://exemple.fr/candidature");
		return p;
	}

	@Test
	void mapsCompleteProposition() {
		JobOpportunity job = mapper.toJobOpportunity(completeProposition());

		assertNotNull(job);
		assertEquals("AB-2026_0001", job.getJobId());
		assertEquals("doctorat-gouv-dev", job.getOrganisationIDKey());
		assertNotNull(job.getLastmodifieddate());

		Description desc = job.getDescription();
		assertEquals("Titre de la thèse", desc.getJobTitle());
		assertEquals("Short summary", desc.getJobSummary());
		assertEquals(DisciplineEnum.MATHEMATICS, desc.getResearchField().get(0).getMainResearchField());
		assertEquals("Applied physics", desc.getResearchField().get(0).getSubResearchField());
		assertEquals(LanguagesLevelEnum.EXCELLENT,
				job.getAdditionalRequirements().getRequiredLanguages().stream()
						.filter(r -> r.getLanguage() == LanguagesEnum.ENGLISH)
						.map(RequiredLanguages::getLanguageLevel).findFirst().orElse(null));
		assertEquals(LanguagesLevelEnum.MOTHER_TONGUE,
				job.getAdditionalRequirements().getRequiredLanguages().stream()
						.filter(r -> r.getLanguage() == LanguagesEnum.FRENCH)
						.map(RequiredLanguages::getLanguageLevel).findFirst().orElse(null));

		assertEquals("Laboratoire de Recherche", job.getWorkLocation().get(0).getJobOrganisationInstitute());
		assertEquals("Université Exemple", job.getHiringOrgInst().getOrganisationInstitute());
		assertEquals("deposant@exemple.fr", job.getHiringOrgInst().getEMail().get(0));

		ApplicationDetails app = job.getApplicationDetails();
		assertEquals(HowToApplyEnum.WEBSITE, app.getHowToApply());
		assertEquals("https://exemple.fr/candidature", app.getApplicationWebsite());
	}

	@Test
	void fallsBackOnEnglishTitleWhenEnglishResumeMissing() {
		PropositionThese p = completeProposition();
		p.setResumeAnglais(null);

		JobOpportunity job = mapper.toJobOpportunity(p);

		assertNotNull(job);
		assertEquals("Thesis title", job.getDescription().getJobSummary());
	}

	@Test
	void fallsBackOnEstablishmentWhenResearchUnitMissing() {
		PropositionThese p = completeProposition();
		p.setUniteRechercheLibelle(null);

		JobOpportunity job = mapper.toJobOpportunity(p);

		assertNotNull(job);
		assertEquals("Université Exemple", job.getWorkLocation().get(0).getJobOrganisationInstitute());
	}

	@Test
	void fallsBackOnContactEmailWhenDeposantEmailMissing() {
		PropositionThese p = completeProposition();
		p.setDeposantEmail(null);
		p.setDirectionTheseEmail(null);

		JobOpportunity job = mapper.toJobOpportunity(p);

		assertNotNull(job);
		assertEquals("contact@doctorat.gouv.fr", job.getHiringOrgInst().getEMail().get(0));
	}

	@Test
	void omitsLanguagesWhenEmptyOrAucun() {
		PropositionThese p = completeProposition();
		p.setNiveauAnglaisRequis("");
		p.setNiveauFrancaisRequis("Aucun");

		JobOpportunity job = mapper.toJobOpportunity(p);

		assertNotNull(job);
		assertNull(job.getAdditionalRequirements(),
				"Aucune langue requise → element additional-requirements omis");
	}

	@Test
	void excludesWhenTitleMissing() {
		PropositionThese p = completeProposition();
		p.setTheseTitre(null);

		assertNull(mapper.toJobOpportunity(p));
	}

	@Test
	void excludesWhenDeadlineMissing() {
		PropositionThese p = completeProposition();
		p.setDateLimiteCandidature(null);

		assertNull(mapper.toJobOpportunity(p));
	}

	@Test
	void excludesWhenEstablishmentMissing() {
		PropositionThese p = completeProposition();
		p.setEtablissementLibelle(null);
		p.setUniteRechercheLibelle(null);

		assertNull(mapper.toJobOpportunity(p));
	}

	@Test
	void truncatesFieldsBeyondXsdMax() {
		PropositionThese p = completeProposition();
		p.setMatricule("A".repeat(80));
		p.setTheseTitre("T".repeat(300));

		JobOpportunity job = mapper.toJobOpportunity(p);

		assertNotNull(job);
		assertEquals(50, job.getJobId().length(), "job-id limité à 50 (textInputID)");
		assertEquals(255, job.getDescription().getJobTitle().length(), "job-title limité à 255");
	}

	@Test
	void clearsInvalidJobIdCharacters() {
		PropositionThese p = completeProposition();
		p.setMatricule("Matricule / avec [accents] et #dièse!");

		JobOpportunity job = mapper.toJobOpportunity(p);

		assertNotNull(job);
		assertTrue(job.getJobId().matches("[A-Za-z0-9\\s()_-]*"));
	}
}
