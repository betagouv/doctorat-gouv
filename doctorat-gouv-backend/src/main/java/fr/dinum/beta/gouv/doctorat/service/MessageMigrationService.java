package fr.dinum.beta.gouv.doctorat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.entity.MessageEchange;
import fr.dinum.beta.gouv.doctorat.repository.MessageEchangeRepository;

/**
 * Migration une fois des messages historiques restés en clair vers le format chiffré.
 * Le simple fait de relire + sauver chaque entité suffit : le converter JPA
 * chiffre à l'écriture.
 */
@Service
public class MessageMigrationService {

    private static final Logger log = LoggerFactory.getLogger(MessageMigrationService.class);

    private final MessageEchangeRepository messageRepository;

    public MessageMigrationService(MessageEchangeRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public int migrateLegacyMessages(int batchSize) {
        int total = 0;
        int page = 0;
        while (true) {
            Page<MessageEchange> legacy = messageRepository.findLegacyCleartext(PageRequest.of(page, batchSize));
            if (legacy.isEmpty()) {
                break;
            }
            // Les entités sont managées : le dirty-checking + converter rechiffre au flush.
            // saveAll explicite pour forcer le passage par convertToDatabaseColumn.
            messageRepository.saveAll(legacy.getContent());
            messageRepository.flush();
            total += legacy.getNumberOfElements();
            log.info("Messages migrés vers le format chiffré : {} (page {})", total, page);
            if (!legacy.hasNext()) {
                break;
            }
            page++;
        }
        log.info("Migration chiffrement terminée : {} message(s) migré(s)", total);
        return total;
    }
}
