package fr.dinum.beta.gouv.doctorat.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * En cas d'erreur interne, renvoie un flux XML vide valide (jamais d'erreur
 * HTTP) afin de ne pas casser la récupération périodique du flux par EURAXESS.
 */
@RestControllerAdvice(assignableTypes = EuraxessController.class)
public class EuraxessFeedExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(EuraxessFeedExceptionHandler.class);

	private static final String EMPTY_FEED =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?><job-opportunities isIncremental=\"true\"/>";

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handle(Exception ex) {
		log.error("Erreur lors de la génération du flux EURAXESS", ex);
		return ResponseEntity.ok()
				.contentType(MediaType.TEXT_XML)
				.body(EMPTY_FEED);
	}

}
