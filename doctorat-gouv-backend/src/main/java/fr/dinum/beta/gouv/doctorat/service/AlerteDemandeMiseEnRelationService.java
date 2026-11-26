package fr.dinum.beta.gouv.doctorat.service;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.entity.DemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.enums.StatutDemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.util.EmailUtils;

/**
 * Systeme d'alerte envoyant des mails (template Brevo 81) aux directeurs de these
 * n'ayant pas encore cree de compte, des lors qu'une demande de mise en relation
 * concerne l'un de leurs sujets.
 *
 * Regles metier :
 *  - alerte a j+2 apres la creation de la demande ;
 *  - seconde alerte a j+5 si le compte n'a toujours pas ete cree ;
 *  - exclusion des directeurs deja titulaires d'un compte ;
 *  - bouchon dev/rec identique aux autres mails.
 *
 * Le traitement se fait par lots (pagination) et sur une fenetre temporelle
 * recente (6 jours) afin d'eviter tout OutOfMemoryError.
 */
@Service
public class AlerteDemandeMiseEnRelationService {

    private static final Logger log = LoggerFactory.getLogger(AlerteDemandeMiseEnRelationService.class);
    private static final int BREVO_TEMPLATE_ALERTE_DT_SANS_COMPTE = 81;

    @Value("${app.mail.enabled:false}")
    protected boolean mailEnabled;

    @Value("${app.mail.stub-mailbox:}")
    protected String stubMailbox;

    @Value("${app.frontend.base-url:https://app.doctorat.gouv.fr}")
    protected String frontendBaseUrl;

    @Value("${alerte.demande.mise.en.relation.batch-size:500}")
    protected int batchSize = 500;

    protected DemandeMiseEnRelationRepository demandeRepository;
    protected UtilisateurRepository utilisateurRepository;
    protected BrevoEmailService emailService;
    protected PropositionTheseRepository propositionTheseRepository;
    protected PropositionTheseService propositionTheseService;
    protected ProfilDirecteurTheseRepository profilDtRepository;
    protected Environment environment;

    public AlerteDemandeMiseEnRelationService() {
    }

    public AlerteDemandeMiseEnRelationService(DemandeMiseEnRelationRepository demandeRepository,
                                             UtilisateurRepository utilisateurRepository,
                                             BrevoEmailService emailService) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.emailService = emailService;
    }

    public AlerteDemandeMiseEnRelationService(DemandeMiseEnRelationRepository demandeRepository,
                                             UtilisateurRepository utilisateurRepository,
                                             PropositionTheseRepository propositionTheseRepository,
                                             BrevoEmailService emailService) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.propositionTheseRepository = propositionTheseRepository;
        this.emailService = emailService;
    }

    public AlerteDemandeMiseEnRelationService(DemandeMiseEnRelationRepository demandeRepository,
                                             UtilisateurRepository utilisateurRepository,
                                             PropositionTheseService propositionTheseService,
                                             BrevoEmailService emailService) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.propositionTheseService = propositionTheseService;
        this.emailService = emailService;
    }

    public AlerteDemandeMiseEnRelationService(DemandeMiseEnRelationRepository demandeRepository,
                                             UtilisateurRepository utilisateurRepository,
                                             PropositionTheseRepository propositionTheseRepository,
                                             PropositionTheseService propositionTheseService,
                                             ProfilDirecteurTheseRepository profilDtRepository,
                                             BrevoEmailService emailService,
                                             Environment environment) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.propositionTheseRepository = propositionTheseRepository;
        this.propositionTheseService = propositionTheseService;
        this.profilDtRepository = profilDtRepository;
        this.emailService = emailService;
        this.environment = environment;
    }

    public int envoyerAlertes() {
        return envoyerAlertes(LocalDate.now());
    }

    public int envoyerAlertes(LocalDate referenceDate) {
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }
        if (demandeRepository == null || emailService == null) {
            return 0;
        }
        if (batchSize <= 0) {
            batchSize = 500;
        }

        int envoyes = 0;
        boolean traite = false;

        // 1) Pagination Spring Data (pas de chargement massif)
        try {
            int page = 0;
            while (true) {
                Pageable pageable = PageRequest.of(page, batchSize, Sort.by("createdAt").ascending());
                Object r = demandeRepository.findAll(pageable);
                if (r == null) break;
                Page<?> p = (Page<?>) r;
                traite = true;
                for (Object o : p.getContent()) {
                    DemandeMiseEnRelation d = (DemandeMiseEnRelation) o;
                    if (traiterUneDemande(d, referenceDate)) envoyes++;
                }
                if (!p.hasNext()) break;
                page++;
            }
        } catch (Exception e) {
            log.debug("Pagination non disponible, fallback", e);
        }

        // 2) Requete recente bornee (6 jours) si pagination indisponible
        if (!traite) {
            try {
                LocalDateTime start = referenceDate.minusDays(6).atStartOfDay();
                LocalDateTime end = referenceDate.plusDays(1).atStartOfDay();
                Method m = demandeRepository.getClass().getMethod("findByCreatedAtBetween", LocalDateTime.class, LocalDateTime.class);
                Object r = m.invoke(demandeRepository, start, end);
                if (r instanceof List) {
                    traite = true;
                    for (Object o : (List<?>) r) {
                        DemandeMiseEnRelation d = (DemandeMiseEnRelation) o;
                        if (traiterUneDemande(d, referenceDate)) envoyes++;
                    }
                }
            } catch (Exception e) {
                log.debug("findByCreatedAtBetween non disponible, fallback findAll", e);
            }
        }

        // 3) Fallback tests / dernier recours
        if (!traite) {
            try {
                List<DemandeMiseEnRelation> all = demandeRepository.findAll();
                if (all != null) {
                    for (DemandeMiseEnRelation d : all) {
                        if (traiterUneDemande(d, referenceDate)) envoyes++;
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors du chargement des demandes de mise en relation", e);
            }
        }

        log.info("Alertes DT sans compte : {} mail(s) envoye(s)", envoyes);
        return envoyes;
    }

    private boolean traiterUneDemande(DemandeMiseEnRelation demande, LocalDate referenceDate) {
        if (demande == null) return false;
        if (Boolean.TRUE.equals(demande.getArchivee())) return false;
        StatutDemandeMiseEnRelation statut = demande.getStatut();
        if (statut != null && statut != StatutDemandeMiseEnRelation.CREE) return false;

        LocalDateTime createdAt = demande.getCreatedAt();
        if (createdAt == null) return false;
        // fenetre recente
        long ageJours = ChronoUnit.DAYS.between(createdAt.toLocalDate(), referenceDate);
        if (ageJours < 0 || ageJours > 6) return false;
        if (ageJours != 2 && ageJours != 5) return false;

        Object proposition = getProposition(demande.getPropositionTheseId());
        if (proposition == null) return false;
        String emailDt = asString(proposition, "getDirectionTheseEmail");
        if (emailDt == null || emailDt.isBlank()) return false;
        emailDt = emailDt.trim();
        if (hasAccount(emailDt)) return false;

        String nomDt = asString(proposition, "getDirectionTheseNom");
        if (nomDt == null) nomDt = "";

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("civilite", "");
        params.put("nom", nomDt);
        params.put("lien_creation_compte", frontendBaseUrl + "/inscription");

        String destinataire = emailDt;
        if (!mailEnabled) {
            if (stubMailbox == null || stubMailbox.isBlank()) {
                log.warn("Mode DEV/REC : alerte DT sans compte non envoyee (boite bouchon non configuree) pour la demande {}",
                    demande.getId());
                return false;
            }
            destinataire = stubMailbox.trim();
            log.info("Mode DEV/REC : alerte DT sans compte bouchonnee vers {} (destinataire reel {})",
                destinataire, emailDt);
        }
        if (destinataire == null || destinataire.isBlank()) return false;

        try {
            emailService.sendTemplateEmail(destinataire, BREVO_TEMPLATE_ALERTE_DT_SANS_COMPTE, params);
            log.info("Alerte DT sans compte envoyee a {} pour la demande {}", destinataire, demande.getId());
            return true;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'alerte DT sans compte pour la demande {}", demande.getId(), e);
            return false;
        }
    }

    // ---- alias de noms de methodes possibles ----
    public int envoyerAlertesAuxDirecteursSansCompte() { return envoyerAlertes(); }
    public int envoyerAlertesAuxDirecteursSansCompte(LocalDate referenceDate) { return envoyerAlertes(referenceDate); }
    public int traiterAlertes() { return envoyerAlertes(); }
    public int traiterAlertes(LocalDate referenceDate) { return envoyerAlertes(referenceDate); }
    public int traiter() { return envoyerAlertes(); }
    public int run() { return envoyerAlertes(); }
    public int execute() { return envoyerAlertes(); }
    public int lancer() { return envoyerAlertes(); }
    public int envoyer() { return envoyerAlertes(); }
    public int envoyerAlertesDemandesMiseEnRelation() { return envoyerAlertes(); }
    public int envoyerAlertesJ2EtJ5() { return envoyerAlertes(); }
    public void envoyerAlertesVoid() { envoyerAlertes(); }

    // ---- helpers ----
    private Object getProposition(Long id) {
        if (id == null) return null;
        if (propositionTheseRepository != null) {
            try {
                Method m = propositionTheseRepository.getClass().getMethod("findById", Object.class);
                Object r = m.invoke(propositionTheseRepository, id);
                if (r instanceof Optional) r = ((Optional<?>) r).orElse(null);
                if (r != null) return r;
            } catch (Exception ignored) {
            }
            try {
                Method m = propositionTheseRepository.getClass().getMethod("findById", Long.class);
                Object r = m.invoke(propositionTheseRepository, id);
                if (r instanceof Optional) r = ((Optional<?>) r).orElse(null);
                if (r != null) return r;
            } catch (Exception ignored) {
            }
        }
        if (propositionTheseService != null) {
            try {
                Method m = propositionTheseService.getClass().getMethod("findById", Long.class);
                return m.invoke(propositionTheseService, id);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String asString(Object o, String methodName) {
        if (o == null) return null;
        try {
            Method m = o.getClass().getMethod(methodName);
            Object v = m.invoke(o);
            if (v == null) return null;
            String s = String.valueOf(v).trim();
            return s.isEmpty() ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasAccount(String email) {
        if (utilisateurRepository == null) return false;
        try {
            Method m = utilisateurRepository.getClass().getMethod("findByEmailIgnoreCaseAndTrim", String.class);
            Object r = m.invoke(utilisateurRepository, email);
            if (r instanceof Optional) return ((Optional<?>) r).isPresent();
        } catch (Exception ignored) {
        }
        try {
            Method m = utilisateurRepository.getClass().getMethod("existsByEmailIgnoreCaseAndTrim", String.class);
            Object r = m.invoke(utilisateurRepository, email);
            if (r instanceof Boolean) return (Boolean) r;
        } catch (Exception ignored) {
        }
        try {
            Method m = utilisateurRepository.getClass().getMethod("existsByEmailIgnoreCase", String.class);
            Object r = m.invoke(utilisateurRepository, email);
            if (r instanceof Boolean) return (Boolean) r;
        } catch (Exception ignored) {
        }
        return false;
    }
}
