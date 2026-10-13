package fr.dinum.beta.gouv.doctorat.service.storage;

/**
 * Échec technique de stockage (écriture, lecture, suppression).
 * Ne contient jamais de secret (clés AK/SK exclues des messages).
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
