package fr.dinum.beta.gouv.doctorat.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import fr.dinum.beta.gouv.doctorat.service.storage.StorageException;
import fr.dinum.beta.gouv.doctorat.service.storage.StoragePort;

/**
 * Service de stockage des photos de profil.
 * Valide le format (magic bytes JPEG/PNG/WebP) et la taille, puis délègue au
 * backend configuré ({@code storage.backend} : disque local en dev/test,
 * OUTSCALE OOS chiffré SSE-S3 en rec/prod). La clé objet est générée
 * côté serveur ({@code photos/{userId}/{UUID}.{ext}}) — jamais dérivée du
 * nom de fichier client.
 */
@Service
public class PhotoService {

    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);

    private static final long PHOTO_MAX_SIZE = 2_097_152L; // 2 Mo

    /** Formats acceptés → contentType + extension normalisée. */
    private static final Set<String> FORMATS_ACCEPTES = Set.of("image/jpeg", "image/png", "image/webp");

    private final StoragePort storage;

    public PhotoService(StoragePort storage) {
        this.storage = storage;
    }

    /**
     * Stocke la photo de profil de l'utilisateur. La clé retournée est à
     * persister dans {@code Utilisateur.photoUrl}.
     */
    public String storePhoto(String userId, MultipartFile file) {
        String contentType = valider(file);
        String ext = extension(contentType);
        String key = "photos/" + userId + "/" + UUID.randomUUID() + "." + ext;
        return storage.store(key, readBytes(file), contentType);
    }

    /**
     * Supprime la photo du stockage. L'échec est loggé sans faire échouer
     * l'opération (suppression au mieux, comme pour les pièces).
     */
    public void deletePhotoQuietly(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            storage.delete(key);
        } catch (Exception e) {
            log.warn("Suppression de la photo {} impossible (orphelin résiduel)", key, e);
        }
    }

    public boolean exists(String key) {
        return key != null && !key.isBlank() && storage.exists(key);
    }

    public java.io.InputStream open(String key) {
        return storage.open(key);
    }

    public long size(String key) {
        return storage.size(key);
    }

    private String valider(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo : aucun fichier fourni");
        }
        if (file.getSize() > PHOTO_MAX_SIZE) {
            throw new IllegalArgumentException("Photo : la taille du fichier est trop importante (2 Mo maximum)");
        }
        byte[] bytes = readBytes(file);
        String contentType = detecterFormat(bytes);
        if (contentType == null) {
            throw new IllegalArgumentException("Photo : seul un fichier JPEG, PNG ou WebP est accepté");
        }
        return contentType;
    }

    /**
     * Détecte le format sur les magic bytes (le Content-Type et le nom du
     * fichier client ne sont pas fiables).
     */
    private String detecterFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }
        // JPEG : FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // PNG : 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A) {
            return "image/png";
        }
        // WebP : "RIFF" .... "WEBP"
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private String extension(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new StorageException("Erreur lors de la lecture du fichier", e);
        }
    }
}
