package fr.dinum.beta.gouv.doctorat.controller;

import fr.dinum.beta.gouv.doctorat.dto.AllFilterOptions;
import fr.dinum.beta.gouv.doctorat.service.FilterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/filters")
public class FilterController {

    private final FilterService filterService;

    public FilterController(FilterService filterService) {
        this.filterService = filterService;
    }

    /**
     * GET /api/filters/all
     * Retourne un JSON contenant les cinq listes d’options.
     */
    @GetMapping("/all")
    public AllFilterOptions getAllFilters() {
        return filterService.getAllFilters();
    }

}
