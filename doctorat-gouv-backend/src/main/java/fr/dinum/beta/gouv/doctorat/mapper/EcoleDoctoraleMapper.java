package fr.dinum.beta.gouv.doctorat.mapper;

import fr.dinum.beta.gouv.doctorat.dto.EcoleDoctoraleDto;
import fr.dinum.beta.gouv.doctorat.entity.EcoleDoctorale;

public class EcoleDoctoraleMapper {
	
	public static EcoleDoctoraleDto toDto(EcoleDoctorale entity) {
		if (entity == null) return null;
		
		EcoleDoctoraleDto dto = new EcoleDoctoraleDto();
		
		dto.setId(entity.getId());
		dto.setNumero(entity.getNumero());
		dto.setLibelle(entity.getLibelle());
		dto.setEtablissementRor(entity.getEtablissementRor());
		dto.setEtablissementLibelle(entity.getEtablissementLibelle());
		dto.setUai(entity.getUai());
		dto.setAcademie(entity.getAcademie());
		dto.setRegionAcademie(entity.getRegionAcademie());
		dto.setSecteur(entity.getSecteur());
		dto.setSpecialite(entity.getSpecialite());
		dto.setActive(entity.getActive());
		dto.setDateSynchronisation(entity.getDateSynchronisation());
		dto.setFresqRecordId(entity.getFresqRecordId());
		
		return dto;
	}
	
	public static EcoleDoctorale toEntity(EcoleDoctoraleDto dto) {
		if (dto == null) return null;
		
		EcoleDoctorale entity = new EcoleDoctorale();
		
		entity.setId(dto.getId());
		entity.setNumero(dto.getNumero());
		entity.setLibelle(dto.getLibelle());
		entity.setEtablissementRor(dto.getEtablissementRor());
		entity.setEtablissementLibelle(dto.getEtablissementLibelle());
		entity.setUai(dto.getUai());
		entity.setAcademie(dto.getAcademie());
		entity.setRegionAcademie(dto.getRegionAcademie());
		entity.setSecteur(dto.getSecteur());
		entity.setSpecialite(dto.getSpecialite());
		entity.setActive(dto.getActive());
		entity.setDateSynchronisation(dto.getDateSynchronisation());
		entity.setFresqRecordId(dto.getFresqRecordId());
		
		return entity;
	}
}
