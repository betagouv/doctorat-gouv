package fr.dinum.beta.gouv.doctorat.dto;

public record TheseSemanticDocument(
        Long id,
        String matricule,
        String texteComplet
) {
	// Ce record est un simple conteneur de données pour le document sémantique d'une thèse.
	// Il contient l'identifiant de la thèse, son matricule et le texte complet à indexer.
}

