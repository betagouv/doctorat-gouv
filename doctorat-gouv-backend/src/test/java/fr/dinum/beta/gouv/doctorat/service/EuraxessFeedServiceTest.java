package fr.dinum.beta.gouv.doctorat.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

/**
 * Test d'intégration du service de flux EURAXESS sur base H2 embarquée :
 * offres actives publiées, offres inactives ignorées, cas base vide.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class EuraxessFeedServiceTest {

	@Autowired
	private EuraxessFeedService euraxessFeedService;

	@Autowired
	private PropositionTheseRepository repository;

	@Test
	void throwsWhenNoActiveOffer() {
		EuraxessEmptyFeedException ex = assertThrows(EuraxessEmptyFeedException.class,
				() -> euraxessFeedService.generateFeed(),
				"Base vide → exception, l'endpoint répondra 204 No Content");
		assertTrue(ex.getMessage().contains("Aucune offre"));
	}

	@Test
	void publishesActiveOfferAndIgnoresInactive() {
		PropositionThese active = new PropositionThese();
		active.setMatricule("act-0001");
		active.setTypeProposition("proposition");
		active.setActive(true);
		active.setTheseTitre("Sujet actif");
		active.setResume("Description du sujet actif");
		active.setDateMaj(LocalDateTime.of(2026, 8, 1, 10, 0));
		active.setDateLimiteCandidature(LocalDateTime.of(2026, 10, 1, 0, 0));
		active.setDomaineScientifique("2");
		active.setSpecialite("physique");
		active.setEtablissementLibelle("Université Exemple");
		active.setEtablissementVille("Paris");
		active.setEtablissementCodePostal("75000");
		active.setDeposantEmail("deposant@exemple.fr");
		repository.save(active);

		PropositionThese inactive = new PropositionThese();
		inactive.setMatricule("ina-0002");
		inactive.setTypeProposition("proposition");
		inactive.setActive(false);
		inactive.setTheseTitre("Sujet inactif");
		inactive.setResume("Ne doit pas apparaître");
		inactive.setDateMaj(LocalDateTime.of(2026, 8, 1, 11, 0));
		inactive.setDateLimiteCandidature(LocalDateTime.of(2026, 10, 1, 0, 0));
		inactive.setDomaineScientifique("2");
		inactive.setEtablissementLibelle("Université Exemple");
		repository.save(inactive);

		String xml = euraxessFeedService.generateFeed();

		assertTrue(xml.contains("act-0001"), "L'offre active doit être publiée");
		assertTrue(!xml.contains("ina-0002"), "L'offre inactive ne doit pas être publiée");
		assertTrue(xml.contains("isIncremental=\"false\""),
				"Offres présentes → flux complet attendu, obtenu : " + xml);
	}

	@Test
	void exclutOffreAccompagnementInterneMemeSiComplete() {
		// Offre DOCTORAT_GOUV remplissant tous les champs obligatoires EURAXESS
		// (y compris la deadline) : elle ne doit en aucun cas être diffusée.
		PropositionThese offre = new PropositionThese();
		offre.setMatricule("DG-11111111");
		offre.setTypeProposition("offre");
		offre.setSource(SourceThese.DOCTORAT_GOUV);
		offre.setActive(true);
		offre.setTheseTitre("Offre d'accompagnement");
		offre.setResume("Description de l'offre d'accompagnement");
		offre.setDateMaj(LocalDateTime.of(2026, 8, 1, 10, 0));
		offre.setDateLimiteCandidature(LocalDateTime.of(2026, 10, 1, 0, 0));
		offre.setDomaineScientifique("2");
		offre.setSpecialite("physique");
		offre.setEtablissementLibelle("Université Exemple");
		offre.setEtablissementVille("Paris");
		offre.setEtablissementCodePostal("75000");
		offre.setDeposantEmail("directeur@exemple.fr");
		repository.save(offre);

		EuraxessEmptyFeedException ex = assertThrows(EuraxessEmptyFeedException.class,
				() -> euraxessFeedService.generateFeed(),
				"Offre DOCTORAT_GOUV seule en base → flux vide attendu");
		assertTrue(ex.getMessage().contains("Aucune offre"));
	}
}
