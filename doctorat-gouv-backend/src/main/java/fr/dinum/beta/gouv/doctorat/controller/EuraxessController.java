package fr.dinum.beta.gouv.doctorat.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.service.EuraxessFeedService;

@RestController
@RequestMapping("/api/euraxess")
public class EuraxessController {

	private final EuraxessFeedService euraxessFeedService;

	public EuraxessController(EuraxessFeedService euraxessFeedService) {
		this.euraxessFeedService = euraxessFeedService;
	}

	/**
	 * Flux XML EURAXESS des sujets de thèse actifs.
	 * Exemple d'appel : GET /api/euraxess/feed
	 */
	@GetMapping(value = "/feed", produces = MediaType.TEXT_XML_VALUE)
	public ResponseEntity<String> feed() {
		return ResponseEntity.ok().contentType(MediaType.TEXT_XML)
				.body(euraxessFeedService.generateFeed());
	}

}
