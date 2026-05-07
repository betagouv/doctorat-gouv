package fr.dinum.beta.gouv.doctorat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.service.TheseIndexationService;

@Component
@Profile("test") // Ce runner ne s'exécutera que dans le profil "test"
public class AlbertDocumentTestRunner implements ApplicationRunner {
	
	private static final Logger log = LoggerFactory.getLogger(AlbertDocumentTestRunner.class);

	private final PropositionTheseRepository repository;
	private final TheseIndexationService indexationService;

	public AlbertDocumentTestRunner(PropositionTheseRepository repository, TheseIndexationService indexationService) {
		this.repository = repository;
		this.indexationService = indexationService;
	}

    @Transactional
	@Override
	public void run(ApplicationArguments args) {
		log.info("Lancement du test d'indexation Albert...");
		var sujets = repository.findActivePropositions().stream().limit(3).toList();

		for (PropositionThese sujet : sujets) {
			indexationService.indexerDocumentSiNecessaire(sujet);
			log.info("Document Albert créé pour sujet ID " + sujet.getId());
		}
		log.info("Test d'indexation Albert terminé.");
	}
}
