package fr.dinum.beta.gouv.doctorat.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proposition_these")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class PropositionThese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 11, nullable = false, unique = true)
    private String matricule;

    @Column(length = 12, nullable = false)
    private String typeProposition;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreation;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateMaj;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateSoumission;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateMiseEnLigne;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateLimiteCandidature;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebutThese;

    @Column(length = 16)
    private String deposantOrcid;

    @Column(length = 50)
    private String deposantNom;

    @Column(length = 25)
    private String deposantPrenom;

    @Column(length = 100)
    private String deposantEmail;

    @Column(length = 4)
    private String anneeUniversitaire;

    @Column(length = 3)
    private String ecoleDoctoralNumero;

    @Column(length = 9)
    private String uniteRechercheRor;

    @Column(length = 300)
    private String uniteRechercheLibelle;

    @Column(length = 7)
    private String uniteRechercheCodePostal;

    @Column(length = 40)
    private String uniteRechercheVille;

    @Column(length = 9)
    private String etablissementRor;

    @Column(length = 250)
    private String etablissementLibelle;

    @Column(length = 7)
    private String etablissementCodePostal;

    @Column(length = 40)
    private String etablissementVille;

    @Column(length = 255)
    private String specialite;

    private String domaineScientifique;

    @Column(length = 16)
    private String directionTheseOrcid;

    @Column(length = 50)
    private String directionTheseNom;

    @Column(length = 25)
    private String directionThesePrenom;

    @Column(length = 100)
    private String directionTheseEmail;

    @Column(length = 16)
    private String codirectionTheseOrcid;

    @Column(length = 50)
    private String codirectionTheseNom;

    @Column(length = 25)
    private String codirectionThesePrenom;

    @Column(length = 100)
    private String codirectionTheseEmail;

    @Column(length = 3)
    private String interdisciplinaire;

    @Column(length = 3)
    private String cotutelle;

    @Column(length = 3)
    private String cotutellePaysCode;

    @Column(columnDefinition = "TEXT")
    private String modalitesEncadrement;

    @Column(length = 3)
    private String domainesImpact;

    @ElementCollection
    private List<String> domainesImpactListe;

    @Column(length = 3)
    private String objectifsDeveloppementDurable;

    @ElementCollection
    private List<String> objectifsDeveloppementDurableListe;

    @Column(length = 255)
    private String theseTitre;

    @ElementCollection
    private Map<String, String> motsCles;

    @Column(length = 255)
    private String theseTitreAnglais;

    @ElementCollection
    private Map<String, String> motsClesAnglais;

    @Column(columnDefinition = "TEXT")
    private String resume;

    @Column(columnDefinition = "TEXT")
    private String resumeAnglais;

    @Column(columnDefinition = "TEXT")
    private String thematiqueRecherche;

    @Column(columnDefinition = "TEXT")
    private String domaine;

    @Column(columnDefinition = "TEXT")
    private String objectif;

    @Column(columnDefinition = "TEXT")
    private String contexte;

    @Column(columnDefinition = "TEXT")
    private String methodeDeTravail;

    @Column(columnDefinition = "TEXT")
    private String resultatsAttendus;

    @Column(columnDefinition = "TEXT")
    private String referencesBibliographiques;

    @Column(columnDefinition = "TEXT")
    private String conditionsMaterielles;

    @Column(columnDefinition = "TEXT")
    private String ouvertureInternationale;

    @Column(columnDefinition = "TEXT")
    private String collaborationsEnvisagees;

    @Column(columnDefinition = "TEXT")
    private String valorisationTravaux;

    @Column(length = 3)
    private String confidentiel;

    @Column(length = 255)
    private String urlInfosComplementaires;

    @Column(length = 255)
    private String financementEtat;

    @ElementCollection
    private List<String> financementTypes;

    @Column(length = 255)
    private String financementEmployeur;

    @Column(length = 255)
    private String financementOrigine;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate financementDateDebut;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate financementDateFin;

    @Column(columnDefinition = "TEXT")
    private String financementDetails;

    @Column(columnDefinition = "TEXT")
    private String profilRecherche;

    @Column(columnDefinition = "TEXT")
    private String profilRechercheAnglais;

    @Column(length = 5)
    private String niveauAnglaisRequis;

    @Column(length = 5)
    private String niveauFrancaisRequis;

    @Column(length = 3)
    private String candidatureEnLignePossible;

}


