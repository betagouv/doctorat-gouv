package fr.dinum.beta.gouv.doctorat.service;

import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.AllFilterOptions;
import fr.dinum.beta.gouv.doctorat.enums.DomaineScientifique;
import fr.dinum.beta.gouv.doctorat.enums.RegionsFrance;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

@Service
public class FilterService {

    private final PropositionTheseRepository repo;

    public FilterService(PropositionTheseRepository repo) {
        this.repo = repo;
    }

    /** Récupère toutes les listes d’options en un seul appel */
    public AllFilterOptions getAllFilters() {
        // 1️ - Listes simples existantes
        List<String> disciplines   = getDomainesScientifiquesLibelles();
        List<String> localisations = getLocalisationsRegions();
        List<String> laboratoires = repo.findDistinctLaboratoires();
        List<String> ecoles       = repo.findDistinctEcoles();

        // 2️ - Nouvelles listes « Défis de société » (union + déduplication)
        List<String> domainesImpact   = repo.findDistinctDomainesImpact();
        List<String> objectifsDurables = repo.findDistinctObjectifsDurables();

        List<String> defisSociete = Stream.concat(
                domainesImpact.stream(),
                objectifsDurables.stream())
                .filter(Objects::nonNull)          // élimine les nulls éventuels
                .map(String::trim)                 // enlève les espaces superflus
                .filter(s -> !s.isEmpty())
                .distinct()                        // supprime les doublons
                .sorted()                          // optionnel : ordre alphabétique
                .collect(Collectors.toList());

        // 3️ - Retour du DTO
        return new AllFilterOptions(
                disciplines,
                localisations,
                laboratoires,
                ecoles,
                defisSociete);
    }
    
	/**
	 * Récupère la liste des libellés des domaines scientifiques à partir de leurs codes.
	 * @return
	 */
	public List<String> getDomainesScientifiquesLibelles() {
		List<String> codes = repo.findDistinctDomainesScientifiques();

		return codes.stream().
				map(DomaineScientifique::labelFromCode)
				.filter(Objects::nonNull)
				.sorted()
				.toList();
	}
	
	/**
	 * Récupère la liste des régions à partir des codes postaux des localisations existantes dans la base de données, en mettant d'abord les régions actives (celles qui ont au moins un sujet) et en triant le tout par ordre alphabétique.
	 * @return
	 */
	public List<String> getLocalisationsRegions() {
	    List<String> codesPostaux = repo.findDistinctLocalisations();

	    Set<String> regionsAvecSujets = codesPostaux.stream()
	            .map(RegionsFrance::regionFromCodePostal)
	            .filter(Objects::nonNull)
	            .collect(Collectors.toSet());

	    Collator collator = Collator.getInstance(Locale.FRENCH);
	    collator.setStrength(Collator.PRIMARY);

	    return RegionsFrance.allRegions().stream()
	            .sorted((r1, r2) -> {
	                boolean a1 = regionsAvecSujets.contains(r1);
	                boolean a2 = regionsAvecSujets.contains(r2);
	                if (a1 != a2) return a1 ? -1 : 1; // actives d'abord
	                return collator.compare(r1, r2);
	            })
	            .toList();
	}
	
	/**
	 * Récupère la liste des régions à partir des codes postaux des localisations existantes l'enum RegionsFrance.
	 * @return
	 */
	public List<String> getAllLocalisationsRegionsFromEnum() {
	    Collator collator = Collator.getInstance(Locale.FRENCH);
	    collator.setStrength(Collator.PRIMARY);

	    return RegionsFrance.allRegions().stream()
	            .sorted(collator)
	            .toList();
	}

	
	/**
	 * Récupère la liste des régions à partir des codes postaux des localisations existantes dans la base de données.
	 * @return
	 */
	public List<String> getLocalisationsRegionsOnlyExistingInDB() {
	    List<String> codesPostaux = repo.findDistinctLocalisations();
	    
	    Collator collator = Collator.getInstance(Locale.FRENCH); 
	    collator.setStrength(Collator.PRIMARY); // ignore accents

	    return codesPostaux.stream()
	            .map(RegionsFrance::regionFromCodePostal)
	            .filter(Objects::nonNull)
	            .distinct()
	            .sorted(collator)
	            .toList();
	}


}
