package fr.dinum.beta.gouv.doctorat.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.util.EmailUtils;

/**
 * Composant d'autorisation pour le rattachement directeur de thèse ↔ sujet.
 * Factorise la logique de rattachement utilisée par DirecteurTheseService et EchangeService.
 *
 * Le rattachement se fait uniquement par e-mail (V1) : l'ORCID est saisi librement
 * par l'utilisateur et n'est pas vérifié. Il pourra être réintroduit via un claim PRC
 * vérifié (Option B).
 */
@Service
public class DirecteurTheseAuthorizationService {

    private final UtilisateurRepository utilisateurRepository;
    private final ProfilDirecteurTheseRepository profilDtRepository;
    private final PropositionTheseRepository propositionTheseRepository;

    public DirecteurTheseAuthorizationService(UtilisateurRepository utilisateurRepository,
                                             ProfilDirecteurTheseRepository profilDtRepository,
                                             PropositionTheseRepository propositionTheseRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.profilDtRepository = profilDtRepository;
        this.propositionTheseRepository = propositionTheseRepository;
    }

    /**
     * Retourne les sujets rattachés au directeur de thèse identifié par userId.
     * Le rattachement se fait par comparaison de l'e-mail du DT avec les champs
     * direction/codirection des propositions de thèse.
     */
    public Map<Long, PropositionThese> findSujetsRattaches(String userId) {
        Map<Long, PropositionThese> sujets = new LinkedHashMap<>();

        Utilisateur utilisateur = utilisateurRepository.findById(userId).orElse(null);
        if (utilisateur == null) {
            return sujets;
        }

        String email = EmailUtils.normalize(utilisateur.getEmail());
        if (email == null) {
            return sujets;
        }

        List<PropositionThese> parEmail = propositionTheseRepository.findByDirecteurEmail(email);
        if (parEmail != null) {
            for (PropositionThese p : parEmail) {
                if (p.getId() != null) {
                    sujets.putIfAbsent(p.getId(), p);
                }
            }
        }
        return sujets;
    }
}