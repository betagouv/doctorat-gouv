package fr.dinum.beta.gouv.doctorat.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.entity.MessageEchange;
import fr.dinum.beta.gouv.doctorat.repository.MessageEchangeRepository;
import jakarta.persistence.EntityManager;

/**
 * Vérifie que le contenu est chiffré au repos (colonne brute)
 * et transparent à la lecture applicative.
 */
@SpringBootTest
@Transactional
class MessageEchangeEncryptionIT {

    @Autowired
    private MessageEchangeRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void contenu_chiffreEnBaseEtLisibleEnClairViaJpa() {
        String clair = "Proposition : thèse sur l'IA Mohan — test intégration ✓";
        MessageEchange message = new MessageEchange(999L, "candidat-1", clair);
        MessageEchange sauve = repository.saveAndFlush(message);

        // Colonne brute (bypass du converter via SQL natif)
        String brut = (String) entityManager
            .createNativeQuery("SELECT contenu FROM message_echange WHERE id = :id")
            .setParameter("id", sauve.getId())
            .getSingleResult();

        assertTrue(brut.startsWith(MessageCryptoService.PREFIX),
            "La colonne brute doit être chiffrée (préfixe ENC1:), reçu : " + brut);
        assertFalse(brut.contains("Proposition"),
            "Le clair ne doit pas fuiter dans la colonne");

        entityManager.clear();
        MessageEchange relu = repository.findById(sauve.getId()).orElseThrow();
        assertEquals(clair, relu.getContenu());
    }
}
