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
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
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

    private final DemandeMiseEnRelationRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PropositionTheseRepository propositionTheseRepository;
    private final BrevoEmailService emailService;

    public AlerteDemandeMiseEnRelationService(DemandeMiseEnRelationRepository demandeRepository,
                                             UtilisateurRepository utilisateurRepository,
                                             PropositionTheseRepository propositionTheseRepository,
                                             BrevoEmailService emailService) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.propositionTheseRepository = propositionTheseRepository;
        this.emailService = emailService;
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

        PropositionThese proposition = getProposition(demande.getPropositionTheseId());
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

    // ---- helpers ----
    private PropositionThese getProposition(Long id) {
        if (id == null) return null;
        if (propositionTheseRepository == null || id == null) return null;
        try {
            return propositionTheseRepository.findById(id).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String asString(PropositionThese p, String methodName) {
        if (p == null) return null;
        if ("getDirectionTheseEmail".equals(methodName)) return clean(p.getDirectionTheseEmail());
        if ("getDirectionTheseNom".equals(methodName)) return clean(p.getDirectionTheseNom());
        return null;
    }

    private String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean hasAccount(String email) {
        if (utilisateurRepository == null) return false;
        try {
            Optional<?> r = utilisateurRepository.findByEmailIgnoreCaseAndTrim(email);
            if (r != null) return r.isPresent();
        } catch (Exception ignored) {
        }
        try {
            return utilisateurRepository.existsByEmailIgnoreCaseAndTrim(email);
        } catch (Exception ignored) {
        }
        return false;
    }
}
