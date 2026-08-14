package fr.dinum.beta.gouv.doctorat.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.dinum.beta.gouv.doctorat.service.EuraxessEmptyFeedException;

/**
 * Répond {@code 204 No Content} dès que le flux ne peut pas être généré :
 * aucune offre à publier ({@link EuraxessEmptyFeedException}) ou erreur
 * interne. Aucun XML vide n'est émis : il ne serait pas conforme au XSD
 * ({@code minOccurs="1"} sur {@code job-opportunity}) et EURAXESS le
 * rejetterait à la validation.
 */
@RestControllerAdvice(assignableTypes = EuraxessController.class)
public class EuraxessFeedExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(EuraxessFeedExceptionHandler.class);

	@ExceptionHandler(EuraxessEmptyFeedException.class)
	public ResponseEntity<Void> handleEmptyFeed(EuraxessEmptyFeedException ex) {
		log.warn("{} : réponse 204 No Content", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Void> handle(Exception ex) {
		log.error("Erreur lors de la génération du flux EURAXESS : réponse 204 No Content", ex);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

}
