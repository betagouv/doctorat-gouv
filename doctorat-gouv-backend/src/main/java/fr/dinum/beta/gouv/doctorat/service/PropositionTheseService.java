package fr.dinum.beta.gouv.doctorat.service;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.mapper.PropositionTheseMapper;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PropositionTheseService {

    private final PropositionTheseRepository propositionTheseRepository;

    public PropositionTheseService(PropositionTheseRepository propositionTheseRepository) {
        this.propositionTheseRepository = propositionTheseRepository;
    }

    /** Recherche paginée avec filtres dynamiques */
    public Page<PropositionTheseDto> search(Map<String, String> filters,
                                            int page,
                                            int size) {
        Specification<PropositionThese> spec = buildSpecification(filters);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "dateCreation")); // tri par date de création

        Page<PropositionThese> entities = propositionTheseRepository.findAll(spec, pageable);
        return entities.map(PropositionTheseMapper::toDto);
    }

    /** Construction de la Specification à partir des paramètres */
    private Specification<PropositionThese> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            query.distinct(true);                         // éviter les doublons dûs aux JOIN
            List<Predicate> andPredicates = new ArrayList<>(); // chaque token → un AND

            // ---------- JOINTURES SUR LES MAPS ----------
            MapJoin<PropositionThese, String, String> joinMotsCles =
                    root.joinMap("motsCles", JoinType.LEFT);
            MapJoin<PropositionThese, String, String> joinMotsClesAnglais =
                    root.joinMap("motsClesAnglais", JoinType.LEFT);

            // ---------- TRAITEMENT DES AUTRES FILTERS ----------
            filters.forEach((key, value) -> {
                if (value == null || value.isBlank()) return;

                String lowered = value.toLowerCase();
                String pattern = "%" + lowered + "%";

                switch (key) {
                    case "discipline" -> andPredicates.add(cb.equal(root.get("specialite"), value));
                    case "thematique" -> andPredicates.add(cb.equal(root.get("domaine"), value));
                    case "localisation" -> andPredicates.add(cb.equal(root.get("uniteRechercheVille"), value));
                    case "laboratoire" -> andPredicates.add(cb.like(cb.lower(root.get("uniteRechercheLibelle")), pattern));
                    case "ecole" -> andPredicates.add(cb.equal(root.get("etablissementLibelle"), value));
                    case "query" -> {
                        // ----------- TOKENISATION -------------
                        String[] tokens = value.trim().toLowerCase().split("\\s+");
                        List<Predicate> tokenPredicates = new ArrayList<>();

                        for (String token : tokens) {
                            if (token.isBlank()) continue;
                            String tokenPattern = "%" + token + "%";

                            // OR entre les champs pour le même token
                            Predicate tokenInAnyField = cb.or(
                                    cb.like(cb.lower(root.get("theseTitre")), tokenPattern),
                                    cb.like(cb.lower(root.get("theseTitreAnglais")), tokenPattern),
                                    cb.like(cb.lower(root.get("resume")), tokenPattern),
                                    cb.like(cb.lower(root.get("resumeAnglais")), tokenPattern),
                                    cb.like(cb.lower(joinMotsCles.value()), tokenPattern),
                                    cb.like(cb.lower(joinMotsClesAnglais.value()), tokenPattern)
                            );

                            tokenPredicates.add(tokenInAnyField); // chaque token → un predicate
                        }

                        // AND entre les tokens (tous les tokens doivent être trouvés)
                        if (!tokenPredicates.isEmpty()) {
                            andPredicates.add(cb.and(tokenPredicates.toArray(Predicate[]::new)));
                        }
                    }
                    default -> { /* ignore unknown keys */ }
                }
            });

            // Si aucun filtre n’est fourni → renvoie tout
            return andPredicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(andPredicates.toArray(Predicate[]::new));
        };
    }
}
