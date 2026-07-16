package fr.dinum.beta.gouv.doctorat.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.ExportPropositionTheseDTO;
import fr.dinum.beta.gouv.doctorat.dto.ExportResponseDTO;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final PropositionTheseRepository propositionTheseRepository;

    public ExportService(PropositionTheseRepository propositionTheseRepository) {
        this.propositionTheseRepository = propositionTheseRepository;
    }

    public ExportResponseDTO exportPropositionsActives(Integer page, Integer size) {
        if (page == null || size == null) {
            log.info("Export complet des propositions actives");

            List<PropositionThese> all = propositionTheseRepository.findByActiveTrue(
                    PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "dateMaj")))
                    .getContent();

            log.info("Export complet retourné {} résultats", all.size());

            return new ExportResponseDTO(
                    all.stream().map(this::toDTO).toList(),
                    0,
                    all.size(),
                    all.size(),
                    1
            );
        }

        size = Math.min(size, 500);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateMaj"));

        log.info("Export paginé des propositions actives - page={}, size={}", page, size);

        Page<PropositionThese> result = propositionTheseRepository.findByActiveTrue(pageable);

        log.info("Export paginé retourné {} résultats sur {} total", result.getContent().size(), result.getTotalElements());

        return new ExportResponseDTO(
                result.getContent().stream().map(this::toDTO).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private ExportPropositionTheseDTO toDTO(PropositionThese entity) {
        return new ExportPropositionTheseDTO(
                entity.getMatricule(),
                entity.getSource(),
                entity.getTypeProposition(),
                entity.getTheseTitre(),
                entity.getTheseTitreAnglais(),
                entity.getEcoleDoctoraleNumero(),
                entity.getEcoleDoctoraleLibelle(),
                entity.getUniteRechercheRor(),
                entity.getUniteRechercheLibelle(),
                entity.getUniteRechercheCodePostal(),
                entity.getUniteRechercheVille(),
                entity.getEtablissementRor(),
                entity.getEtablissementLibelle(),
                entity.getEtablissementCodePostal(),
                entity.getEtablissementVille(),
                entity.getSpecialite(),
                entity.getDomaineScientifique(),
                entity.getDirectionTheseOrcid(),
                entity.getDirectionTheseNom(),
                entity.getDirectionThesePrenom(),
                entity.getDirectionTheseEmail(),
                entity.getCodirectionTheseOrcid(),
                entity.getCodirectionTheseNom(),
                entity.getCodirectionThesePrenom(),
                entity.getCodirectionTheseEmail(),
                entity.getInterdisciplinaire(),
                entity.getCotutelle(),
                entity.getCotutellePaysCode(),
                entity.getModalitesEncadrement(),
                entity.getResume(),
                entity.getResumeAnglais(),
                entity.getThematiqueRecherche(),
                entity.getDomaine(),
                entity.getObjectif(),
                entity.getContexte(),
                entity.getMethodeDeTravail(),
                entity.getResultatsAttendus(),
                entity.getReferencesBibliographiques(),
                entity.getConditionsMaterielles(),
                entity.getOuvertureInternationale(),
                entity.getCollaborationsEnvisagees(),
                entity.getValorisationTravaux(),
                entity.getDomainesImpact(),
                entity.getObjectifsDeveloppementDurable(),
                entity.getFinancementEtat(),
                entity.getFinancementEmployeur(),
                entity.getFinancementOrigine(),
                entity.getFinancementDetails(),
                entity.getProfilRecherche(),
                entity.getProfilRechercheAnglais(),
                entity.getNiveauAnglaisRequis(),
                entity.getNiveauFrancaisRequis(),
                entity.getCandidatureEnLignePossible(),
                entity.getUrlInfosComplementaires(),
                entity.getUrlPdf(),
                entity.getUrlCandidature(),
                entity.getSujetAttribue(),
                entity.getConfidentiel(),
                entity.getAnnee(),
                entity.getDateCreation(),
                entity.getDateMaj(),
                entity.getDateSoumission(),
                entity.getDateMiseEnLigne(),
                entity.getDateLimiteCandidature(),
                entity.getActive()
        );
    }
}
