package fr.dinum.beta.gouv.doctorat.service;

import fr.dinum.beta.gouv.doctorat.dto.AllFilterOptions;
import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.exception.ResourceNotFoundException;
import fr.dinum.beta.gouv.doctorat.mapper.PropositionTheseMapper;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PropositionTheseService {

    private final PropositionTheseRepository repo;

    public PropositionTheseService(PropositionTheseRepository repo) {
        this.repo = repo;
    }

    /* --------------------------------------------------------------
       Méthode exposée au contrôleur – renvoie toutes les listes de filtres
       -------------------------------------------------------------- */
    public AllFilterOptions getAllFilters() {
        List<String> disciplines   = repo.findDistinctDisciplines();
        List<String> localisations= repo.findDistinctLocalisations();
        List<String> laboratoires = repo.findDistinctLaboratoires();
        List<String> ecoles       = repo.findDistinctEcoles();

        // ---- Nouveaux filtres « Défis de société » -----------------
        List<String> domainesImpact   = repo.findDistinctDomainesImpact();
        List<String> objectifsDurables= repo.findDistinctObjectifsDurables();

        // Fusion + suppression des doublons
        List<String> defisSociete = Stream.concat(domainesImpact.stream(),
                                                   objectifsDurables.stream())
                                          .filter(Objects::nonNull)
                                          .map(String::trim)
                                          .filter(s -> !s.isEmpty())
                                          .distinct()
                                          .sorted()
                                          .collect(Collectors.toList());

        return new AllFilterOptions(disciplines, localisations,
                                    laboratoires, ecoles, defisSociete);
    }

    /* --------------------------------------------------------------
       Recherche paginée avec filtres dynamiques (y compris « Défis »)
       -------------------------------------------------------------- */
    public Page<PropositionTheseDto> search(Map<String, String> filters,
                                            int page,
                                            int size) {
        Specification<PropositionThese> spec = buildSpecification(filters);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "dateCreation"));

        Page<PropositionThese> entities = repo.findAll(spec, pageable);
        return entities.map(PropositionTheseMapper::toDto);
    }

    /** Construction de la Specification */
    private Specification<PropositionThese> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            query.distinct(true);                     // éviter les doublons liés aux JOIN
            List<Predicate> andPredicates = new ArrayList<>();

            // JOINTURES sur les deux listes (utilisées uniquement si le filtre « defisSociete » est présent)
            Join<PropositionThese, String> joinDomainesImpact =
                    root.joinList("domainesImpactListe", JoinType.LEFT);
            Join<PropositionThese, String> joinObjectifsDurables =
                    root.joinList("objectifsDeveloppementDurableListe", JoinType.LEFT);

            filters.forEach((key, value) -> {
                if (value == null || value.isBlank()) return;
                String lowered = value.toLowerCase();
                String pattern = "%" + lowered + "%";

                switch (key) {
                    case "discipline" -> andPredicates.add(cb.equal(root.get("specialite"), value));
                    case "localisation" -> andPredicates.add(cb.equal(root.get("uniteRechercheVille"), value));
                    case "laboratoire" -> andPredicates.add(cb.like(cb.lower(root.get("uniteRechercheLibelle")), pattern));
                    case "ecole" -> andPredicates.add(cb.equal(root.get("etablissementLibelle"), value));

                    // -------------------------------------------------
                    // Nouveau filtre « Défis de société »
                    // -------------------------------------------------
                    case "defisSociete" -> {
                        // Le défi peut être présent soit dans domainesImpactListe,
                        // soit dans objectifsDeveloppementDurableListe.
                        Predicate pDomaines   = cb.like(cb.lower(joinDomainesImpact), pattern);
                        Predicate pObjectifs  = cb.like(cb.lower(joinObjectifsDurables), pattern);
                        andPredicates.add(cb.or(pDomaines, pObjectifs));
                    }

                    // -------------------------------------------------
                    // Recherche texte libre (déjà existante)
                    // -------------------------------------------------
                    case "query" -> {
                        String[] tokens = value.trim().toLowerCase().split("\\s+");
                        List<Predicate> tokenPredicates = new ArrayList<>();

                        for (String token : tokens) {
                            if (token.isBlank()) continue;
                            String tokenPattern = "%" + token + "%";

                            // OR entre les différents champs pour le même token
                            Predicate tokenInAnyField = cb.or(
                                    cb.like(cb.lower(root.get("theseTitre")), tokenPattern),
                                    cb.like(cb.lower(root.get("theseTitreAnglais")), tokenPattern),
                                    cb.like(cb.lower(root.get("resume")), tokenPattern),
                                    cb.like(cb.lower(root.get("resumeAnglais")), tokenPattern)
                                    // les maps de mots‑clés sont déjà traitées dans le filtre « defisSociete »,
                                    // mais on les garde aussi ici au cas où vous voudriez les rechercher
                                    // directement via le champ libre.
                            );
                            tokenPredicates.add(tokenInAnyField);
                        }
                        if (!tokenPredicates.isEmpty()) {
                            andPredicates.add(cb.and(tokenPredicates.toArray(Predicate[]::new)));
                        }
                    }

                    default -> { /* ignore unknown keys */ }
                }
            });

            return andPredicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(andPredicates.toArray(Predicate[]::new));
        };
    }
    
    /**
     * Retourne la proposition de thèse correspondant à l’identifiant fourni.
     *
     * @param id identifiant numérique de la thèse
     * @return DTO de la thèse
     * @throws ResourceNotFoundException si aucune thèse n’est trouvée
     */
    public PropositionTheseDto findById(Long id) {
        Optional<PropositionThese> opt = repo.findById(id);
        return opt.map(PropositionTheseMapper::toDto)               // mapper entité → DTO
                  .orElseThrow(() -> new ResourceNotFoundException(
                          "Proposition de thèse avec l’id " + id + " introuvable"));
    }
}