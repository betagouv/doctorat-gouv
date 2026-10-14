package fr.dinum.beta.gouv.doctorat.dto;

/**
 * Un fichier du candidat exposé sur son profil (nom affichable + taille en octets).
 * Ne contient jamais la clé objet interne ni le chemin de stockage.
 * Même forme que les fichiers côté directeur de thèse.
 */
public class FichierDto {

    private String filename;
    private Long size;

    public FichierDto() {}

    public FichierDto(String filename, Long size) {
        this.filename = filename;
        this.size = size;
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
