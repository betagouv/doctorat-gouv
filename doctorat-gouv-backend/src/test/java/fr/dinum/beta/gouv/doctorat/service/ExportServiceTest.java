package fr.dinum.beta.gouv.doctorat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.dto.ExportResponseDTO;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

/**
 * L'export REST ne doit en aucun cas contenir les offres d'accompagnement
 * internes (source DOCTORAT_GOUV).
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class ExportServiceTest {

	@Autowired
	private ExportService exportService;

	@Autowired
	private PropositionTheseRepository repository;

	private static PropositionThese proposition(String matricule, SourceThese source) {
		PropositionThese p = new PropositionThese();
		p.setMatricule(matricule);
		p.setTypeProposition(source == SourceThese.DOCTORAT_GOUV ? "offre" : "proposition");
		p.setSource(source);
		p.setActive(true);
		p.setTheseTitre("Titre " + matricule);
		p.setDateMaj(LocalDateTime.of(2026, 8, 1, 10, 0));
		return p;
	}

	@Test
	void exportExclutOffresAccompagnementInternes() {
		repository.save(proposition("AB-2026_0001", SourceThese.ADUM));
		repository.save(proposition("DG-11111111", SourceThese.DOCTORAT_GOUV));

		ExportResponseDTO export = exportService.exportPropositionsActives(null, null);

		assertEquals(1, export.content().size());
		assertEquals("AB-2026_0001", export.content().get(0).matricule());
		assertTrue(export.content().stream().noneMatch(d -> d.source() == SourceThese.DOCTORAT_GOUV));
	}

	@Test
	void exportPagineExclutOffresAccompagnementInternes() {
		repository.save(proposition("AB-2026_0001", SourceThese.ADUM));
		repository.save(proposition("DG-11111111", SourceThese.DOCTORAT_GOUV));

		ExportResponseDTO export = exportService.exportPropositionsActives(0, 10);

		assertEquals(1, export.totalElements());
		assertEquals("AB-2026_0001", export.content().get(0).matricule());
	}
}
