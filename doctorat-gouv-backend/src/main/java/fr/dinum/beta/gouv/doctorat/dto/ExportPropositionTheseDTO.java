package fr.dinum.beta.gouv.doctorat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportPropositionTheseDTO(
        String matricule,
        SourceThese source,
        String typeProposition,
        String theseTitre,
        String theseTitreAnglais,
        String ecoleDoctoraleNumero,
        String ecoleDoctoraleLibelle,
        String uniteRechercheRor,
        String uniteRechercheLibelle,
        String uniteRechercheCodePostal,
        String uniteRechercheVille,
        String etablissementRor,
        String etablissementLibelle,
        String etablissementCodePostal,
        String etablissementVille,
        String specialite,
        String domaineScientifique,
        String directionTheseOrcid,
        String directionTheseNom,
        String directionThesePrenom,
        String directionTheseEmail,
        String codirectionTheseOrcid,
        String codirectionTheseNom,
        String codirectionThesePrenom,
        String codirectionTheseEmail,
        String interdisciplinaire,
        String cotutelle,
        String cotutellePaysCode,
        String modalitesEncadrement,
        String resume,
        String resumeAnglais,
        String thematiqueRecherche,
        String domaine,
        String objectif,
        String contexte,
        String methodeDeTravail,
        String resultatsAttendus,
        String referencesBibliographiques,
        String conditionsMaterielles,
        String ouvertureInternationale,
        String collaborationsEnvisagees,
        String valorisationTravaux,
        String domainesImpact,
        List<String> domainesImpactListe,
        String objectifsDeveloppementDurable,
        List<String> objectifsDeveloppementDurableListe,
        String financementEtat,
        List<String> financementTypes,
        String financementEmployeur,
        String financementOrigine,
        LocalDate financementDateDebut,
        LocalDate financementDateFin,
        String financementDetails,
        String profilRecherche,
        String profilRechercheAnglais,
        String niveauAnglaisRequis,
        String niveauFrancaisRequis,
        String candidatureEnLignePossible,
        String urlInfosComplementaires,
        String urlPdf,
        String urlCandidature,
        String sujetAttribue,
        String confidentiel,
        String deposantOrcid,
        String deposantNom,
        String deposantPrenom,
        String deposantEmail,
        LocalDate dateDebutThese,
        String anneeUniversitaire,
        Map<String, String> motsCles,
        Map<String, String> motsClesAnglais,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateCreation,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateMaj,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateSoumission,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateMiseEnLigne,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateLimiteCandidature
) {}
