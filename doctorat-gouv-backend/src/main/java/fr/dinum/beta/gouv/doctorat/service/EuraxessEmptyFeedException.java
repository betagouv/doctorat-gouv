package fr.dinum.beta.gouv.doctorat.service;

/**
 * Levée quand le flux EURAXESS ne contient aucune offre à publier (aucune
 * offre active en base ou toutes exclues). L'endpoint répond alors
 * {@code 204 No Content} plutôt qu'un XML vide non conforme au XSD
 * ({@code minOccurs="1"} sur {@code job-opportunity}).
 */
public class EuraxessEmptyFeedException extends RuntimeException {

	public EuraxessEmptyFeedException(String message) {
		super(message);
	}
}
