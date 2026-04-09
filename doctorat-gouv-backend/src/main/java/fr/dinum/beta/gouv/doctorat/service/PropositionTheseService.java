package fr.dinum.beta.gouv.doctorat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.AllFilterOptions;
import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.enums.DomaineScientifique;
import fr.dinum.beta.gouv.doctorat.enums.RegionsFrance;
import fr.dinum.beta.gouv.doctorat.exception.ResourceNotFoundException;
import fr.dinum.beta.gouv.doctorat.mapper.PropositionTheseMapper;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Service
public class PropositionTheseService {
	
	private static final Logger log = LoggerFactory.getLogger(PropositionTheseService.class);

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
        
        String sortField = filters.getOrDefault("sortField", "dateMiseEnLigne");
        String sortDirection = filters.getOrDefault("sortDirection", "DESC");

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<PropositionThese> entities = repo.findAll(spec, pageable);
        return entities.map(PropositionTheseMapper::toDto);
    }

    /** Construction de la Specification */
	private Specification<PropositionThese> buildSpecification(Map<String, String> filters) {
		return (root, query, cb) -> {
			query.distinct(true); // éviter les doublons liés aux JOIN
			List<Predicate> andPredicates = new ArrayList<>();
			
			// Ne garder que les sujets non désactivés (active = true ou null)
			andPredicates.add(
			    cb.or(
			        cb.isTrue(root.get("active")),
			        cb.isNull(root.get("active"))
			    )
			);

			// JOINTURES sur les deux listes (utilisées uniquement si le filtre
			// « defisSociete » est présent)
			Join<PropositionThese, String> joinDomainesImpact = root.joinList("domainesImpactListe", JoinType.LEFT);
			Join<PropositionThese, String> joinObjectifsDurables = root.joinList("objectifsDeveloppementDurableListe",
					JoinType.LEFT);

			filters.forEach((key, value) -> {
			    if (value == null || value.isBlank()) return;

			    // Découper les valeurs multi-select
			    List<String> values = Stream.of(value.split(";"))
			            .map(String::trim)
			            .filter(v -> !v.isEmpty())
			            .toList();

			    switch (key) {

			        /* ---------------------- DISCIPLINE (multi) ---------------------- */
			        case "discipline" -> {
			            List<String> codes = values.stream()
			                    .map(DomaineScientifique::codeFromLabel)
			                    .filter(Objects::nonNull)
			                    .toList();

			            if (!codes.isEmpty()) {
			                andPredicates.add(root.get("domaineScientifique").in(codes));
			            }
			        }

			        /* ---------------------- LOCALISATION (multi) ---------------------- */
			        case "localisation" -> {
			            List<Predicate> regionPredicates = new ArrayList<>();

			            for (String region : values) {
			                List<String> depts = RegionsFrance.departementsFromRegion(region);

			                List<Predicate> deptPreds = depts.stream()
			                        .map(d -> cb.like(root.get("uniteRechercheCodePostal"), d + "%"))
			                        .toList();

			                regionPredicates.add(cb.or(deptPreds.toArray(Predicate[]::new)));
			            }

			            if (!regionPredicates.isEmpty()) {
			                andPredicates.add(cb.or(regionPredicates.toArray(Predicate[]::new)));
			            }
			        }

			        /* ---------------------- LABORATOIRE (multi) ---------------------- */
			        case "laboratoire" -> {
			            List<Predicate> labPreds = values.stream()
			                    .map(v -> cb.like(cb.lower(root.get("uniteRechercheLibelle")), "%" + v.toLowerCase() + "%"))
			                    .toList();

			            andPredicates.add(cb.or(labPreds.toArray(Predicate[]::new)));
			        }

			        /* ---------------------- ECOLE (multi) ---------------------- */
			        case "ecole" -> {
			            andPredicates.add(root.get("etablissementLibelle").in(values));
			        }

			        /* ---------------------- DEFIS DE SOCIETE (multi) ---------------------- */
			        case "defisSociete" -> {
			            List<Predicate> all = new ArrayList<>();

			            for (String v : values) {
			                String pattern = "%" + v.toLowerCase() + "%";

			                all.add(cb.like(cb.lower(joinDomainesImpact), pattern));
			                all.add(cb.like(cb.lower(joinObjectifsDurables), pattern));
			            }

			            andPredicates.add(cb.or(all.toArray(Predicate[]::new)));
			        }

			        /* ---------------------- ECOLE DOCTORALE (mono) ---------------------- */
			        case "ecoleDoctoraleNumero" ->
			                andPredicates.add(root.get("ecoleDoctoraleNumero").in(values));

			        /* ---------------------- ETABLISSEMENT ROR (mono) ---------------------- */
			        case "etablissementRor" ->
			                andPredicates.add(root.get("etablissementRor").in(values));

			        /* ---------------------- TYPE PROPOSITION (mono) ---------------------- */
			        case "typeProposition" ->
			                andPredicates.add(root.get("typeProposition").in(values));

			        /* ---------------------- ANNEE (multi) ---------------------- */
			        case "annee" -> {
			            List<Integer> years = values.stream()
			                    .map(Integer::valueOf)
			                    .toList();

			            andPredicates.add(root.get("annee").in(years));
			        }

			        /* ---------------------- QUERY (inchangé) ---------------------- */
			        case "query" -> {
			            String[] tokens = value.trim().toLowerCase().split("\\s+");
			            List<Predicate> tokenPredicates = new ArrayList<>();

			            for (String token : tokens) {
			                if (token.isBlank()) continue;
			                String tokenPattern = "%" + token + "%";

			                Predicate tokenInAnyField = cb.or(
			                        cb.like(cb.lower(root.get("theseTitre")), tokenPattern),
			                        cb.like(cb.lower(root.get("theseTitreAnglais")), tokenPattern),
			                        cb.like(cb.lower(root.get("resume")), tokenPattern),
			                        cb.like(cb.lower(root.get("resumeAnglais")), tokenPattern)
			                );
			                tokenPredicates.add(tokenInAnyField);
			            }

			            if (!tokenPredicates.isEmpty()) {
			                andPredicates.add(cb.and(tokenPredicates.toArray(Predicate[]::new)));
			            }
			        }

			        default -> { /* ignore */ }
			    }
			});

			return andPredicates.isEmpty() ? cb.conjunction() : cb.and(andPredicates.toArray(Predicate[]::new));
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