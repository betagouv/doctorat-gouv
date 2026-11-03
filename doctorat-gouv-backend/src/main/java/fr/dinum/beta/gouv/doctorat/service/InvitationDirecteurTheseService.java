package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.entity.InvitationDirecteurThese;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;
import fr.dinum.beta.gouv.doctorat.repository.InvitationDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.util.EmailUtils;

/**
 * Service d'envoi des invitations (template Brevo 64) aux directeurs de these (DT)
 * ayant depose des sujets actifs dans ADUM et/ou AMETHIS.
 *
 * Regles metier :
 *  - un seul mail par DT (deduplication par e-mail normalise, trace dans
 *    invitation_directeur_these) ;
 *  - exclusion des DT deja titulaires d'un compte ;
 *  - sujets actifs uniquement, sources ADUM / AMETHIS (pas les offres internes) ;
 *  - bouchon dev/rec identique aux autres mails.
 *
 * Note d'evolution : pour l'instant seuls les directeurs principaux
 * (directionThese*) sont invites. Il est possible d'etendre aux co-directeurs
 * (codirectionThese*) en ajoutant un second bloc d'extraction dans
 * construireCandidats(...), la deduplication par e-mail se charge du reste.
 */
@Service
public class InvitationDirecteurTheseService {

    private static final Logger log = LoggerFactory.getLogger(InvitationDirecteurTheseService.class);

    private static final int BREVO_TEMPLATE_INVITATION_DT = 64;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.stub-mailbox:}")
    private String stubMailbox;

    @Value("${app.frontend.base-url:https://app.doctorat.gouv.fr}")
    private String frontendBaseUrl;

    private final PropositionTheseRepository propositionTheseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final InvitationDirecteurTheseRepository invitationRepository;
    private final BrevoEmailService emailService;
    private final Environment environment;

    public InvitationDirecteurTheseService(PropositionTheseRepository propositionTheseRepository,
                                          UtilisateurRepository utilisateurRepository,
                                          InvitationDirecteurTheseRepository invitationRepository,
                                          BrevoEmailService emailService,
                                          Environment environment) {
        this.propositionTheseRepository = propositionTheseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.invitationRepository = invitationRepository;
        this.emailService = emailService;
        this.environment = environment;
    }

    /**
     * Envoie les invitations. Retourne le nombre de mails envoyes.
     *
     * @param maxPerRun 0 (ou negatif) = pas de limite ; sinon nombre max par execution.
     */
    public int envoyerInvitations(int maxPerRun) {
        List<PropositionThese> sujets = propositionTheseRepository.findActivePropositions().stream()
            .filter(p -> p.getSource() == SourceThese.ADUM || p.getSource() == SourceThese.AMETHIS)
            .filter(p -> p.getActive() == null || Boolean.TRUE.equals(p.getActive()))
            .toList();

        Map<String, Candidat> candidats = construireCandidats(sujets);

        List<Candidat> aInviter = new ArrayList<>();
        for (Candidat c : candidats.values()) {
            String emailNorm = EmailUtils.normalize(c.email);
            if (emailNorm == null) {
                continue;
            }
            c.email = emailNorm;
            if (invitationRepository.existsByEmail(emailNorm)) {
                continue;
            }
            if (utilisateurRepository.existsByEmailIgnoreCaseAndTrim(emailNorm)) {
                continue;
            }
            aInviter.add(c);
        }

        aInviter.sort(Comparator.comparing(c -> c.email));

        if (maxPerRun > 0 && aInviter.size() > maxPerRun) {
            aInviter = new ArrayList<>(aInviter.subList(0, maxPerRun));
        }

        int envoyes = 0;
        for (Candidat c : aInviter) {
            try {
                envoyerUn(c);
                envoyes++;
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de l'invitation DT pour {}", c.email, e);
            }
        }

        log.info("Invitations DT : {} e-mail(s) envoye(s) sur {} candidat(s) eligible(s)",
            envoyes, candidats.size());
        return envoyes;
    }

    private Map<String, Candidat> construireCandidats(List<PropositionThese> sujets) {
        Map<String, Candidat> result = new LinkedHashMap<>();
        for (PropositionThese p : sujets) {
            // Directeur principal uniquement (voir note d'evolution en en-tete de classe).
            String email = p.getDirectionTheseEmail();
            if (email == null || email.isBlank()) {
                continue;
            }
            String key = EmailUtils.normalize(email);
            if (key == null) {
                continue;
            }
            if (result.containsKey(key)) {
                continue;
            }
            Candidat c = new Candidat();
            c.email = email;
            c.nom = p.getDirectionTheseNom();
            c.prenom = p.getDirectionThesePrenom();
            c.civilite = null;
            c.titreSujet = p.getTheseTitre();
            c.etablissement = p.getEtablissementLibelle();
            c.sourcePlateforme = p.getSource() != null ? p.getSource().name() : null;
            c.propositionTheseId = p.getId();
            result.put(key, c);
        }
        return result;
    }

    private void envoyerUn(Candidat c) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("civilite", c.civilite != null ? c.civilite : "");
        params.put("nom", c.nom != null ? c.nom : "");
        params.put("titre_sujet", c.titreSujet != null ? c.titreSujet : "");
        params.put("etablissement", c.etablissement != null ? c.etablissement : "");
        params.put("source_plateforme", c.sourcePlateforme != null ? c.sourcePlateforme : "");
        params.put("lien_creation_compte", frontendBaseUrl + "/inscription");

        String destinataire = c.email;
        if (!mailEnabled) {
            if (stubMailbox == null || stubMailbox.isBlank()) {
                log.warn("Mode DEV/REC : invitation DT non envoyee (boite bouchon non configuree) pour {}", c.email);
                return;
            }
            destinataire = stubMailbox.trim();
            log.info("Mode DEV/REC : invitation DT bouchonnee vers {} (destinataire reel {})", destinataire, c.email);
        }

        if (destinataire == null || destinataire.isBlank()) {
            return;
        }

        emailService.sendTemplateEmail(destinataire, BREVO_TEMPLATE_INVITATION_DT, params);

        InvitationDirecteurThese trace = InvitationDirecteurThese.builder()
            .email(c.email)
            .nom(c.nom)
            .prenom(c.prenom)
            .civilite(c.civilite)
            .dateEnvoi(LocalDateTime.now())
            .propositionTheseId(c.propositionTheseId)
            .sourcePlateforme(c.sourcePlateforme)
            .build();
        invitationRepository.save(trace);

        log.info("Invitation DT envoyee a {} (sujet {})", destinataire, c.propositionTheseId);
    }

    /** Portee package pour les tests. */
    static class Candidat {
        String email;
        String nom;
        String prenom;
        String civilite;
        String titreSujet;
        String etablissement;
        String sourcePlateforme;
        Long propositionTheseId;
    }
}
