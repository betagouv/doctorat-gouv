package fr.dinum.beta.gouv.doctorat.service;

import fr.dinum.beta.gouv.doctorat.dto.AllFilterOptions;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FilterService {

    private final PropositionTheseRepository repo;

    public FilterService(PropositionTheseRepository repo) {
        this.repo = repo;
    }

    /** Récupère toutes les listes d’options en un seul appel */
    public AllFilterOptions getAllFilters() {
        // 1️ - Listes simples existantes
        List<String> disciplines   = repo.findDistinctDisciplines();
        List<String> localisations = repo.findDistinctLocalisations();
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
}
