package fr.dinum.beta.gouv.doctorat.dto;

/**
 * DTO représentant un résultat de recherche individuel depuis l'API Albert.
 *
 * RGPD : le champ matricule est un identifiant personnel (personne physique)
 * et ne doit PAS être loggué. Le champ content peut contenir des données
 * personnelles extraites du sujet de thèse.
 */
public class AlbertSearchHit {
    private Long propositionTheseId;
    private String matricule;
    private double score;
    private String chunkType;
    private String content;
    private Long albertDocumentId;

    public AlbertSearchHit() {}

    public AlbertSearchHit(Long propositionTheseId, String matricule, double score, String chunkType, String content, Long albertDocumentId) {
        this.propositionTheseId = propositionTheseId;
        this.matricule = matricule;
        this.score = score;
        this.chunkType = chunkType;
        this.content = content;
        this.albertDocumentId = albertDocumentId;
    }

    public Long getPropositionTheseId() { return propositionTheseId; }
    public void setPropositionTheseId(Long propositionTheseId) { this.propositionTheseId = propositionTheseId; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getAlbertDocumentId() { return albertDocumentId; }
    public void setAlbertDocumentId(Long albertDocumentId) { this.albertDocumentId = albertDocumentId; }
}
