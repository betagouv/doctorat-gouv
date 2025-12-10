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

        dto.setTheseTitre(entity.getTheseTitre());
        dto.setResume(entity.getResume());
        dto.setFinancementEtat(entity.getFinancementEtat());

        // 👉 ajoute les autres champs si tu en as besoin
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

        entity.setTheseTitre(dto.getTheseTitre());
        entity.setResume(dto.getResume());
        entity.setFinancementEtat(dto.getFinancementEtat());

        // 👉 ajoute les autres champs si tu en as besoin
        return entity;
    }
}
