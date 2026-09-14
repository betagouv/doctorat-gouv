package fr.dinum.beta.gouv.doctorat.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Entité JPA contenant les attributs spécifiques au profil candidat.
 * Liée à Utilisateur par héritage par jointure (1:1, même ID).
 */
@Entity
@Table(name = "profil_candidat")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilCandidat {

    @Id
    private String id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Utilisateur utilisateur;

    @Column(length = 20)
    private String civilite;

    @Column(length = 50)
    private String situation;

    @Column(length = 20)
    private String telephone;

    @Column(name = "master_confirme")
    private Boolean masterConfirme;

    @Column(name = "cv_filename", length = 500)
    private String cvFilename;

    @ElementCollection
    @CollectionTable(name = "profil_candidat_pieces", joinColumns = @JoinColumn(name = "profil_candidat_id"))
    @Column(name = "piece_path", length = 500)
    @Builder.Default
    private List<String> piecesFilenames = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public String getCivilite() { return civilite; }
    public void setCivilite(String civilite) { this.civilite = civilite; }

    public String getSituation() { return situation; }
    public void setSituation(String situation) { this.situation = situation; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public Boolean getMasterConfirme() { return masterConfirme; }
    public void setMasterConfirme(Boolean masterConfirme) { this.masterConfirme = masterConfirme; }

    public String getCvFilename() { return cvFilename; }
    public void setCvFilename(String cvFilename) { this.cvFilename = cvFilename; }

    public List<String> getPiecesFilenames() { return piecesFilenames; }
    public void setPiecesFilenames(List<String> piecesFilenames) { this.piecesFilenames = piecesFilenames; }
}
