package fr.dinum.beta.gouv.doctorat.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.service.PropositionTheseService;

@RestController
@RequestMapping("/api/propositions-these")
@CrossOrigin(origins = "http://localhost:4200")  // <-- ajoute ça
public class PropositionTheseController {

	private final PropositionTheseService propositionTheseService;

	public PropositionTheseController(PropositionTheseService propositionTheseService) {
		this.propositionTheseService = propositionTheseService;
	}

	/**
	 * Endpoint pour rechercher des propositions de thèse avec filtres optionnels.
	 * Exemple d'appel : GET
	 * /api/propositions-these?discipline=Informatique&localisation=Paris
	 */
	@GetMapping
	public Page<PropositionTheseDto> search(
	    @RequestParam Map<String, String> filters,
	    @RequestParam(name = "page", defaultValue = "0") int page,
	    @RequestParam(name = "size", defaultValue = "27") int size) {
	    return propositionTheseService.search(filters, page, size);
	}

}
