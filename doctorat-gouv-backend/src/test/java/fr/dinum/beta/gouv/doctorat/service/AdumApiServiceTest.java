package fr.dinum.beta.gouv.doctorat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
public class AdumApiServiceTest {

	@Autowired
    private PropositionTheseRepository repository;

    @Autowired
    private AdumApiService adumApiService;

    @Test
    void testInsertUpdateIgnore() {
        // 1. Insérer une première proposition
        PropositionThese p1 = new PropositionThese();
        p1.setMatricule("p10001");
        p1.setTypeProposition("proposition");
        p1.setDateMaj(LocalDateTime.of(2025, 10, 1, 10, 0));
        p1.setTheseTitre("Titre initial");
        repository.save(p1);

        // 2. JSON simulant une mise à jour plus récente
        String jsonUpdate = """
        {
          "propositions": [
            {
              "matricule": "p10001",
              "typeProposition": "proposition",
              "dateMaj": "2025-10-02 12:00:00",
              "theseTitre": "Titre mis à jour"
            }
          ]
        }
        """;

        adumApiService.savePropositionsFromJson(jsonUpdate);

        PropositionThese updated = repository.findByMatricule("p10001").orElseThrow();
        assertEquals("Titre mis à jour", updated.getTheseTitre(),
            "La proposition doit être mise à jour si dateMaj plus récente");

        // 3. JSON simulant une proposition plus ancienne (doit être ignorée)
        String jsonOld = """
        {
          "propositions": [
            {
              "matricule": "p10001",
              "typeProposition": "proposition",
              "dateMaj": "2025-09-30 09:00:00",
              "theseTitre": "Titre obsolète"
            }
          ]
        }
        """;

        adumApiService.savePropositionsFromJson(jsonOld);

        PropositionThese ignored = repository.findByMatricule("p10001").orElseThrow();
        assertEquals("Titre mis à jour", ignored.getTheseTitre(),
            "La proposition ne doit pas être écrasée par une dateMaj plus ancienne");

        // 4. JSON simulant une nouvelle proposition (doit être insérée)
        String jsonNew = """
        {
          "propositions": [
            {
              "matricule": "p20001",
              "typeProposition": "proposition",
              "dateMaj": "2025-11-01 09:00:00",
              "theseTitre": "Nouvelle proposition"
            }
          ]
        }
        """;

        adumApiService.savePropositionsFromJson(jsonNew);

        PropositionThese inserted = repository.findByMatricule("p20001").orElseThrow();
        assertEquals("Nouvelle proposition", inserted.getTheseTitre(),
            "Une nouvelle proposition doit être insérée");
    }
    
}
