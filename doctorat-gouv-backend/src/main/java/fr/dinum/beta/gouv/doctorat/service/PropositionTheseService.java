package fr.dinum.beta.gouv.doctorat.service;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.mapper.PropositionTheseMapper;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import jakarta.persistence.criteria.Predicate;
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateCreation"));

        Page<PropositionThese> entities = propositionTheseRepository.findAll(spec, pageable);
        return entities.map(PropositionTheseMapper::toDto);
    }

    /** Construit la Specification à partir des paramètres reçus */
    private Specification<PropositionThese> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((key, value) -> {
                if (value == null || value.isBlank()) return; // ignore vide

                switch (key) {
                    case "discipline" -> // domaine scientifique (ex. « Sciences », « Médecine », …)
                            predicates.add(cb.equal(root.get("specialite"), value));

                    case "thematique" -> // thématique de recherche
                            predicates.add(cb.equal(root.get("domaine"), value)); // thematiqueRecherche

                    case "localisation" -> // ville du laboratoire / unité de recherche
                            predicates.add(cb.equal(root.get("uniteRechercheVille"), value));

                    case "laboratoire" -> // libellé du laboratoire (recherche partielle, insensible à la casse)
                            predicates.add(cb.like(cb.lower(root.get("uniteRechercheLibelle")),
                                           "%" + value.toLowerCase() + "%"));

                    case "ecole" -> // numéro de l’école doctorale (ou libellé si tu préfères)
                            predicates.add(cb.equal(root.get("etablissementLibelle"), value));

                    case "query" -> {
                        // Recherche texte libre sur le titre et le résumé (case‑insensitive)
                        String pattern = "%" + value.toLowerCase() + "%";
                        Predicate title   = cb.like(cb.lower(root.get("theseTitre")), pattern);
                        Predicate resume  = cb.like(cb.lower(root.get("resume")), pattern);
                        Predicate keywords = cb.like(cb.lower(root.get("resumeAnglais")), pattern);
                        predicates.add(cb.or(title, resume, keywords));
                    }

                    // Si tu ajoutes d’autres filtres (financement, contrat, …) les gérer ici
                    default -> {
                        // ignore les clés inconnues
                    }
                }
            });

            // Combine tous les prédicats avec AND
            return predicates.isEmpty() ? cb.conjunction()
                                        : cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
