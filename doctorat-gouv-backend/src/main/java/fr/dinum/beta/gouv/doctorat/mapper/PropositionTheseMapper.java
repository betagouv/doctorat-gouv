package fr.dinum.beta.gouv.doctorat.mapper;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;

public class PropositionTheseMapper {

    // Conversion Entité -> DTO
    public static PropositionTheseDto toDto(PropositionThese entity) {
        if (entity == null) return null;

        PropositionTheseDto dto = new PropositionTheseDto();

        dto.setId(entity.getId());
        dto.setMatricule(entity.getMatricule());
        dto.setTypeProposition(entity.getTypeProposition());

        dto.setDateCreation(entity.getDateCreation());
        dto.setDateMaj(entity.getDateMaj());
        dto.setDateSoumission(entity.getDateSoumission());
        dto.setDateMiseEnLigne(entity.getDateMiseEnLigne());
        dto.setDateLimiteCandidature(entity.getDateLimiteCandidature());
        dto.setDateDebutThese(entity.getDateDebutThese());

        dto.setDeposantOrcid(entity.getDeposantOrcid());
        dto.setDeposantNom(entity.getDeposantNom());
        dto.setDeposantPrenom(entity.getDeposantPrenom());
        dto.setDeposantEmail(entity.getDeposantEmail());

        dto.setAnneeUniversitaire(entity.getAnneeUniversitaire());
        dto.setEcoleDoctoralNumero(entity.getEcoleDoctoralNumero());

        dto.setUniteRechercheRor(entity.getUniteRechercheRor());
        dto.setUniteRechercheLibelle(entity.getUniteRechercheLibelle());
        dto.setUniteRechercheCodePostal(entity.getUniteRechercheCodePostal());
        dto.setUniteRechercheVille(entity.getUniteRechercheVille());

        dto.setEtablissementRor(entity.getEtablissementRor());
        dto.setEtablissementLibelle(entity.getEtablissementLibelle());
        dto.setEtablissementCodePostal(entity.getEtablissementCodePostal());
        dto.setEtablissementVille(entity.getEtablissementVille());

        dto.setSpecialite(entity.getSpecialite());
        dto.setDomaineScientifique(entity.getDomaineScientifique());

        dto.setDirectionTheseOrcid(entity.getDirectionTheseOrcid());
        dto.setDirectionTheseNom(entity.getDirectionTheseNom());
        dto.setDirectionThesePrenom(entity.getDirectionThesePrenom());
        dto.setDirectionTheseEmail(entity.getDirectionTheseEmail());

        dto.setCodirectionTheseOrcid(entity.getCodirectionTheseOrcid());
        dto.setCodirectionTheseNom(entity.getCodirectionTheseNom());
        dto.setCodirectionThesePrenom(entity.getCodirectionThesePrenom());
        dto.setCodirectionTheseEmail(entity.getCodirectionTheseEmail());

        dto.setInterdisciplinaire(entity.getInterdisciplinaire());
        dto.setCotutelle(entity.getCotutelle());
        dto.setCotutellePaysCode(entity.getCotutellePaysCode());

        dto.setModalitesEncadrement(entity.getModalitesEncadrement());
        dto.setDomainesImpact(entity.getDomainesImpact());
        dto.setDomainesImpactListe(entity.getDomainesImpactListe());

        dto.setObjectifsDeveloppementDurable(entity.getObjectifsDeveloppementDurable());
        dto.setObjectifsDeveloppementDurableListe(entity.getObjectifsDeveloppementDurableListe());

        dto.setTheseTitre(entity.getTheseTitre());
        dto.setMotsCles(entity.getMotsCles());
        dto.setTheseTitreAnglais(entity.getTheseTitreAnglais());
        dto.setMotsClesAnglais(entity.getMotsClesAnglais());

        dto.setResume(entity.getResume());
        dto.setResumeAnglais(entity.getResumeAnglais());
        dto.setThematiqueRecherche(entity.getThematiqueRecherche());
        dto.setDomaine(entity.getDomaine());
        dto.setObjectif(entity.getObjectif());
        dto.setContexte(entity.getContexte());
        dto.setMethodeDeTravail(entity.getMethodeDeTravail());
        dto.setResultatsAttendus(entity.getResultatsAttendus());
        dto.setReferencesBibliographiques(entity.getReferencesBibliographiques());
        dto.setConditionsMaterielles(entity.getConditionsMaterielles());
        dto.setOuvertureInternationale(entity.getOuvertureInternationale());
        dto.setCollaborationsEnvisagees(entity.getCollaborationsEnvisagees());
        dto.setValorisationTravaux(entity.getValorisationTravaux());

        dto.setConfidentiel(entity.getConfidentiel());
        dto.setUrlInfosComplementaires(entity.getUrlInfosComplementaires());

        dto.setFinancementEtat(entity.getFinancementEtat());
        dto.setFinancementTypes(entity.getFinancementTypes());
        dto.setFinancementEmployeur(entity.getFinancementEmployeur());
        dto.setFinancementOrigine(entity.getFinancementOrigine());
        dto.setFinancementDateDebut(entity.getFinancementDateDebut());
        dto.setFinancementDateFin(entity.getFinancementDateFin());
        dto.setFinancementDetails(entity.getFinancementDetails());

        dto.setProfilRecherche(entity.getProfilRecherche());
        dto.setProfilRechercheAnglais(entity.getProfilRechercheAnglais());
        dto.setNiveauAnglaisRequis(entity.getNiveauAnglaisRequis());
        dto.setNiveauFrancaisRequis(entity.getNiveauFrancaisRequis());
        dto.setCandidatureEnLignePossible(entity.getCandidatureEnLignePossible());

        return dto;
    }

    // Conversion DTO -> Entité
    public static PropositionThese toEntity(PropositionTheseDto dto) {
        if (dto == null) return null;

        PropositionThese entity = new PropositionThese();

        entity.setId(dto.getId());
        entity.setMatricule(dto.getMatricule());
        entity.setTypeProposition(dto.getTypeProposition());

        entity.setDateCreation(dto.getDateCreation());
        entity.setDateMaj(dto.getDateMaj());
        entity.setDateSoumission(dto.getDateSoumission());
        entity.setDateMiseEnLigne(dto.getDateMiseEnLigne());
        entity.setDateLimiteCandidature(dto.getDateLimiteCandidature());
        entity.setDateDebutThese(dto.getDateDebutThese());

        entity.setDeposantOrcid(dto.getDeposantOrcid());
        entity.setDeposantNom(dto.getDeposantNom());
        entity.setDeposantPrenom(dto.getDeposantPrenom());
        entity.setDeposantEmail(dto.getDeposantEmail());

        entity.setAnneeUniversitaire(dto.getAnneeUniversitaire());
        entity.setEcoleDoctoralNumero(dto.getEcoleDoctoralNumero());

        entity.setUniteRechercheRor(dto.getUniteRechercheRor());
        entity.setUniteRechercheLibelle(dto.getUniteRechercheLibelle());
        entity.setUniteRechercheCodePostal(dto.getUniteRechercheCodePostal());
        entity.setUniteRechercheVille(dto.getUniteRechercheVille());

        entity.setEtablissementRor(dto.getEtablissementRor());
        entity.setEtablissementLibelle(dto.getEtablissementLibelle());
        entity.setEtablissementCodePostal(dto.getEtablissementCodePostal());
        entity.setEtablissementVille(dto.getEtablissementVille());

        entity.setSpecialite(dto.getSpecialite());
        entity.setDomaineScientifique(dto.getDomaineScientifique());

        entity.setDirectionTheseOrcid(dto.getDirectionTheseOrcid());
        entity.setDirectionTheseNom(dto.getDirectionTheseNom());
        entity.setDirectionThesePrenom(dto.getDirectionThesePrenom());
        entity.setDirectionTheseEmail(dto.getDirectionTheseEmail());

        entity.setCodirectionTheseOrcid(dto.getCodirectionTheseOrcid());
        entity.setCodirectionTheseNom(dto.getCodirectionTheseNom());
        entity.setCodirectionThesePrenom(dto.getCodirectionThesePrenom());
        entity.setCodirectionTheseEmail(dto.getCodirectionTheseEmail());

        entity.setInterdisciplinaire(dto.getInterdisciplinaire());
        entity.setCotutelle(dto.getCotutelle());
        entity.setCotutellePaysCode(dto.getCotutellePaysCode());

        entity.setModalitesEncadrement(dto.getModalitesEncadrement());
        entity.setDomainesImpact(dto.getDomainesImpact());
        entity.setDomainesImpactListe(dto.getDomainesImpactListe());

        entity.setObjectifsDeveloppementDurable(dto.getObjectifsDeveloppementDurable());
        entity.setObjectifsDeveloppementDurableListe(dto.getObjectifsDeveloppementDurableListe());

        entity.setTheseTitre(dto.getTheseTitre());
        entity.setMotsCles(dto.getMotsCles());
        entity.setTheseTitreAnglais(dto.getTheseTitreAnglais());
        entity.setMotsClesAnglais(dto.getMotsClesAnglais());

        entity.setResume(dto.getResume());
        entity.setResumeAnglais(dto.getResumeAnglais());
        entity.setThematiqueRecherche(dto.getThematiqueRecherche());
        entity.setDomaine(dto.getDomaine());
        entity.setObjectif(dto.getObjectif());
        entity.setContexte(dto.getContexte());
        entity.setMethodeDeTravail(dto.getMethodeDeTravail());
        entity.setResultatsAttendus(dto.getResultatsAttendus());
        entity.setReferencesBibliographiques(dto.getReferencesBibliographiques());
        entity.setConditionsMaterielles(dto.getConditionsMaterielles());
        entity.setOuvertureInternationale(dto.getOuvertureInternationale());
        entity.setCollaborationsEnvisagees(dto.getCollaborationsEnvisagees());
        entity.setValorisationTravaux(dto.getValorisationTravaux());

        entity.setConfidentiel(dto.getConfidentiel());
        entity.setUrlInfosComplementaires(dto.getUrlInfosComplementaires());

        entity.setFinancementEtat(dto.getFinancementEtat());
        entity.setFinancementTypes(dto.getFinancementTypes());
        entity.setFinancementEmployeur(dto.getFinancementEmployeur());
        entity.setFinancementOrigine(dto.getFinancementOrigine());
        entity.setFinancementDateDebut(dto.getFinancementDateDebut());
        entity.setFinancementDateFin(dto.getFinancementDateFin());
        entity.setFinancementDetails(dto.getFinancementDetails());

        entity.setProfilRecherche(dto.getProfilRecherche());
        entity.setProfilRechercheAnglais(dto.getProfilRechercheAnglais());
        entity.setNiveauAnglaisRequis(dto.getNiveauAnglaisRequis());
        entity.setNiveauFrancaisRequis(dto.getNiveauFrancaisRequis());
        entity.setCandidatureEnLignePossible(dto.getCandidatureEnLignePossible());

        return entity;
    }
}
