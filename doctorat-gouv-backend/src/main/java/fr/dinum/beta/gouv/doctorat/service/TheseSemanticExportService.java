package fr.dinum.beta.gouv.doctorat.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.TheseSemanticDocument;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

@Service
public class TheseSemanticExportService {
	
	private static final Logger log = LoggerFactory.getLogger(TheseSemanticExportService.class);

	private final PropositionTheseRepository repository;
	private final TheseSemanticBuilder builder;

	public TheseSemanticExportService(PropositionTheseRepository repository, TheseSemanticBuilder builder) {
		this.repository = repository;
		this.builder = builder;
	}

	public List<TheseSemanticDocument> buildAllActive() {
		log.info("Construction des documents sémantiques pour toutes les thèses actives");
		return repository.findActivePropositions().stream().map(builder::build).toList();
	}
}
