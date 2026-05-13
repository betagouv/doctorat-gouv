package fr.dinum.beta.gouv.doctorat;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.service.AlbertSearchService;

@Component
@Profile("test") // Ce runner ne s'exécutera que dans le profil "test"
public class AlbertSearchTestRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(AlbertSearchTestRunner.class);

    private final AlbertSearchService searchService;

    public AlbertSearchTestRunner(AlbertSearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void run(String... args) {

        log.info("=== Test SEARCH Albert ===");

        // String question = "Donne moi le résultat du dernier match de foot entre le PSG et l'OM";
        // String question = "Quels sont les thèses liées à l'intelligence artificelle ?";
        String question = "Quels sont les thèses liées à la technologie et la santé ?";
        
        Map response = searchService.search(question);

        log.info("Réponse brute : {}", response);

        log.info("=== Test SEARCH terminé ===");
    }
}

