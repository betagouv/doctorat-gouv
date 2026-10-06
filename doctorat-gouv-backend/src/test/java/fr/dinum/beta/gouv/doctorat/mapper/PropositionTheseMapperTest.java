package fr.dinum.beta.gouv.doctorat.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;

/**
 * Les nouveaux champs (expertiseMots) doivent traverser le mapper dans les deux sens
 * pour alimenter recherche, fiche détail et exports REST.
 */
class PropositionTheseMapperTest {

    @Test
    void toDto_transmetExpertiseMotsMotsClesEtSource() {
        PropositionThese entity = new PropositionThese();
        entity.setMatricule("DG-11111111");
        entity.setTypeProposition("offre");
        entity.setSource(SourceThese.DOCTORAT_GOUV);
        entity.setTheseTitre("Encadrement IA");
        entity.setExpertiseMots("IA pour la santé");
        entity.setMotsCles(Map.of("0", "IA", "1", "santé"));

        PropositionTheseDto dto = PropositionTheseMapper.toDto(entity);

        assertEquals("IA pour la santé", dto.getExpertiseMots());
        assertEquals(Map.of("0", "IA", "1", "santé"), dto.getMotsCles());
        assertEquals(SourceThese.DOCTORAT_GOUV, dto.getSource());
    }

    @Test
    void toEntity_transmetExpertiseMots() {
        PropositionTheseDto dto = new PropositionTheseDto();
        dto.setMatricule("DG-11111111");
        dto.setExpertiseMots("IA pour la santé");

        PropositionThese entity = PropositionTheseMapper.toEntity(dto);

        assertEquals("IA pour la santé", entity.getExpertiseMots());
    }
}
