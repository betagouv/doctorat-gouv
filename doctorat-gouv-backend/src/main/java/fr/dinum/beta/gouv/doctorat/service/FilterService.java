package fr.dinum.beta.gouv.doctorat.service;

import fr.dinum.beta.gouv.doctorat.dto.AllFilterOptions;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FilterService {

    private final PropositionTheseRepository repo;

    public FilterService(PropositionTheseRepository repo) {
        this.repo = repo;
    }

    /**
     * Récupère toutes les listes d’options en un seul appel.
     */
    public AllFilterOptions getAllFilters() {
        List<String> disciplines   = repo.findDistinctDisciplines();
        List<String> thematics    = repo.findDistinctThematics();
        List<String> localisations= repo.findDistinctLocalisations();
        List<String> laboratoires = repo.findDistinctLaboratoires();
        List<String> ecoles       = repo.findDistinctEcoles();

        return new AllFilterOptions(disciplines, thematics, localisations, laboratoires, ecoles);
    }
}
