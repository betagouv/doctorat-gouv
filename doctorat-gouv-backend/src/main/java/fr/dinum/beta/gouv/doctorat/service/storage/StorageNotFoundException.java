package fr.dinum.beta.gouv.doctorat.service.storage;

/**
 * Clé objet absente ou illisible. Étend {@link IllegalArgumentException} pour rester
 * compatible avec les contrôleurs existants (mappée en 404).
 */
public class StorageNotFoundException extends IllegalArgumentException {

    public StorageNotFoundException(String key) {
        super("Fichier introuvable");
    }
}
