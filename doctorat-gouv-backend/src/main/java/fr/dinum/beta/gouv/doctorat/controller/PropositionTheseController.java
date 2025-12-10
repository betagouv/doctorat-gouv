package fr.dinum.beta.gouv.doctorat.controller;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.service.PropositionTheseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
	public List<PropositionTheseDto> search(@RequestParam Map<String, String> filters) {
		return propositionTheseService.search(filters);
	}
}
