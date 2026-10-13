package fr.dinum.beta.gouv.doctorat.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import fr.dinum.beta.gouv.doctorat.service.storage.StorageException;
import fr.dinum.beta.gouv.doctorat.service.storage.StoragePort;

/**
 * Service de stockage des fichiers uploadés lors de l'inscription (CV + pièces complémentaires).
 * Valide le format (PDF) et la taille, puis délègue au backend configuré
 * ({@code storage.backend} : disque local en dev/test, OUTSCALE OOS en rec/prod).
 * Retourne des clés objet ({@code inscription/{userId}/cv|pieces/{uuid}_{nom}.pdf}),
 * jamais des chemins disque.
 */
@Service
public class InscriptionFileService {

    private static final long CV_MAX_SIZE = 1_048_576L;       // 1 Mo
    private static final long PIECE_MAX_SIZE = 5_242_880L;    // 5 Mo
    private static final int MAX_PIECES = 5;

    private final StoragePort storage;

    public InscriptionFileService(StoragePort storage) {
        this.storage = storage;
    }

    public String storeCv(String userId, MultipartFile file) {
        validatePdf(file, CV_MAX_SIZE, "CV");
        return storage.store(buildKey(userId, "cv", file), readBytes(file), "application/pdf");
    }

    public List<String> storePieces(String userId, List<MultipartFile> files) {
        List<MultipartFile> safe = (files == null) ? List.of() : files;
        if (safe.size() > MAX_PIECES) {
            throw new IllegalArgumentException("Maximum " + MAX_PIECES + " pièces complémentaires autorisées");
        }
        List<String> keys = new ArrayList<>();
        for (MultipartFile file : safe) {
            validatePdf(file, PIECE_MAX_SIZE, "Pièce complémentaire");
            keys.add(storage.store(buildKey(userId, "pieces", file), readBytes(file), "application/pdf"));
        }
        return keys;
    }

    private void validatePdf(MultipartFile file, long maxSize, String label) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(label + " : aucun fichier fourni");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean isPdf = "application/pdf".equalsIgnoreCase(file.getContentType()) || name.endsWith(".pdf");
        if (!isPdf) {
            throw new IllegalArgumentException(label + " : le format PDF est requis");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(label + " : la taille du fichier est trop importante");
        }
    }

    private String buildKey(String userId, String dir, MultipartFile file) {
        String original = file.getOriginalFilename();
        String safeName = (original == null || original.isBlank() ? "fichier.pdf" : original)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        return "inscription/" + userId + "/" + dir + "/" + UUID.randomUUID() + "_" + safeName;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new StorageException("Erreur lors du stockage du fichier", e);
        }
    }
}
