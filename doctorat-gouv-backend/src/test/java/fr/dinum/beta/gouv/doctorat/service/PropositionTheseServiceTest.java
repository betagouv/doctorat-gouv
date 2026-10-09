package fr.dinum.beta.gouv.doctorat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.dinum.beta.gouv.doctorat.dto.OffreEncadrementDto;
import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.ProfilDirecteurThese;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

/**
 * Fiche offre d'encadrement : autres offres du même directeur.
 */
@ExtendWith(MockitoExtension.class)
class PropositionTheseServiceTest {

    @Mock
    private PropositionTheseRepository repo;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private ProfilDirecteurTheseRepository profilDtRepository;

    @InjectMocks
    private PropositionTheseService propositionTheseService;

    private static PropositionThese offre(Long id, String matricule, String email, boolean active) {
        PropositionThese p = new PropositionThese();
        p.setId(id);
        p.setMatricule(matricule);
        p.setTypeProposition("offre");
        p.setSource(SourceThese.DOCTORAT_GOUV);
        p.setTheseTitre("Offre " + matricule);
        p.setDirectionTheseEmail(email);
        p.setActive(active);
        return p;
    }

    @Test
    void findAutresOffresEncadrement_neGardeQueLesOffresActivesDuDirecteur() {
        PropositionThese ref = offre(1L, "DG-AAAAAAAA", "dt@exemple.fr", true);
        PropositionThese autre = offre(2L, "DG-BBBBBBBB", "dt@exemple.fr", true);
        PropositionThese inactive = offre(3L, "DG-CCCCCCCC", "dt@exemple.fr", false);
        PropositionThese sujetAdum = offre(4L, "AB-2026_0001", "dt@exemple.fr", true);
        sujetAdum.setSource(SourceThese.ADUM);
        sujetAdum.setTypeProposition("proposition");

        when(repo.findById(1L)).thenReturn(Optional.of(ref));
        when(repo.findByDirecteurEmail("dt@exemple.fr"))
            .thenReturn(List.of(ref, autre, inactive, sujetAdum));

        List<PropositionTheseDto> result = propositionTheseService.findAutresOffresEncadrement(1L);

        assertEquals(1, result.size());
        assertEquals("DG-BBBBBBBB", result.get(0).getMatricule());
    }

    @Test
    void findAutresOffresEncadrement_referenceInconnueOuNonOffre_retourneVide() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        PropositionThese sujetAdum = new PropositionThese();
        sujetAdum.setId(4L);
        sujetAdum.setSource(SourceThese.ADUM);
        when(repo.findById(4L)).thenReturn(Optional.of(sujetAdum));

        assertTrue(propositionTheseService.findAutresOffresEncadrement(99L).isEmpty());
        assertTrue(propositionTheseService.findAutresOffresEncadrement(4L).isEmpty());
    }

    @Test
    void findOffreEncadrement_enrichitDepuisLeProfil() {
        PropositionThese offre = offre(1L, "DG-AAAAAAAA", "dt@exemple.fr", true);
        offre.setDirectionThesePrenom("Marie");
        offre.setDirectionTheseNom("Curie");

        Utilisateur u = new Utilisateur();
        u.setId("dt-1");
        u.setPhotoUrl("https://exemple.fr/photo.jpg");

        ProfilDirecteurThese profil = new ProfilDirecteurThese();
        profil.setCivilite("Mme");
        profil.setTitre("Directrice de recherche");
        profil.setExpertiseMots("IA pour la santé");
        profil.setOrcid("0000-0001-2345-6789");
        profil.setEtablissement("Université X");
        profil.setLaboratoire("Labo Y");
        profil.setEcoleDoctorale("ED Z");

        when(repo.findById(1L)).thenReturn(Optional.of(offre));
        when(utilisateurRepository.findByEmailIgnoreCaseAndTrim("dt@exemple.fr"))
            .thenReturn(Optional.of(u));
        when(profilDtRepository.findByUtilisateurId("dt-1")).thenReturn(Optional.of(profil));

        OffreEncadrementDto result = propositionTheseService.findOffreEncadrement(1L);

        assertEquals("DG-AAAAAAAA", result.getOffre().getMatricule());
        assertEquals("Mme", result.getDirecteur().getCivilite());
        assertEquals("Marie", result.getDirecteur().getPrenom());
        assertEquals("Curie", result.getDirecteur().getNom());
        assertEquals("Directrice de recherche", result.getDirecteur().getTitre());
        assertEquals("https://exemple.fr/photo.jpg", result.getDirecteur().getPhotoUrl());
        assertEquals("IA pour la santé", result.getDirecteur().getExpertiseMots());
        assertEquals("0000-0001-2345-6789", result.getDirecteur().getOrcid());
        assertEquals("Université X", result.getDirecteur().getEtablissement());
        assertEquals("Labo Y", result.getDirecteur().getLaboratoire());
        assertEquals("ED Z", result.getDirecteur().getEcoleDoctorale());
    }

    @Test
    void findOffreEncadrement_profilIntrouvable_repliSurOffre() {
        PropositionThese offre = offre(1L, "DG-AAAAAAAA", "dt@exemple.fr", true);
        offre.setDirectionThesePrenom("Marie");
        offre.setDirectionTheseNom("Curie");
        offre.setDirectionTheseOrcid("0000-0001-2345-6789");

        when(repo.findById(1L)).thenReturn(Optional.of(offre));
        when(utilisateurRepository.findByEmailIgnoreCaseAndTrim("dt@exemple.fr"))
            .thenReturn(Optional.empty());

        OffreEncadrementDto result = propositionTheseService.findOffreEncadrement(1L);

        assertEquals("Marie", result.getDirecteur().getPrenom());
        assertEquals("0000-0001-2345-6789", result.getDirecteur().getOrcid());
        assertNull(result.getDirecteur().getPhotoUrl());
    }
}
