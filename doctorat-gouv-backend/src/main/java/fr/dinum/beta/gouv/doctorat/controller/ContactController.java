package fr.dinum.beta.gouv.doctorat.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import fr.dinum.beta.gouv.doctorat.dto.Attachment;
import fr.dinum.beta.gouv.doctorat.dto.ContactRequest;
import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.service.BrevoEmailService;
import fr.dinum.beta.gouv.doctorat.service.PropositionTheseService;

/**
 *  Contrôleur chargé de gérer les demandes de contact et d’envoyer des e‑mails via BrevoEmailService.
 */
@RestController
@RequestMapping("/api/contact")
public class ContactController {
	
	private static final Logger log = LoggerFactory.getLogger(ContactController.class);
	
	private static final Map<String, Integer> TEMPLATE_BY_PROFIL = Map.of(
		    "Étudiant au sein d'un master français", 14,
		    "Étudiant d'un master étranger", 19,
		    "Élève d'une école d'ingénieur", 28,
		    "Élève d'une autre grande école conférant le grade master", 20,
		    "Chercheur en entreprise", 21,
		    "Entreprise souhaitant établir un partenariat", 29,
		    "Autre professionnel en activité", 22,
		    "Autre organisation souhaitant établir un partenariat", 30,
		    "Autre", 23
		    // Les autres profils retomberont sur la valeur par défaut
		);
	
	private static final Map<String, Integer> TEMPLATE_BY_TYPE_OFFRE = Map.of(
		    "proposition", 24,
		    "offre", 15
		    // les autres retombent sur la valeur par défaut
		);


	
	@Value("${app.mail.enabled:false}")
	private boolean mailEnabled;

    private final BrevoEmailService emailService;
    
	private final PropositionTheseService propositionTheseService;

    public ContactController(BrevoEmailService emailService, PropositionTheseService propositionTheseService) {
        this.emailService = emailService;
        this.propositionTheseService = propositionTheseService;
    }

    /**
     * Méthode POST pour envoyer des e‑mails à partir des demandes de contact. 
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<?> sendMails(@RequestBody ContactRequest request) {
    	log.info("Réception d'une demande de contact le : " + java.time.LocalDateTime.now());
    	
    	// 1 - Validation du contexte 
    	validateRequest(request);
    	
    	// 2 - Envoi des mails
    	if (mailEnabled) {
    	    templateMailEncadrant(request);
    	    templateMailCandidat(request);
    	} else {
    	    log.warn("Mode DEV : mails désactivés");
    	}

        return ResponseEntity.ok(Map.of("message", "Email envoyé"));
    }


	/**
	 * Prépare et envoie un e‑mail à l’encadrant en utilisant un template.
	 * @param req
	 */
	private void templateMailEncadrant(ContactRequest req) {

	    log.info("Préparation template mail encadrant");

	    Map<String, Object> params = new HashMap<>();
	    params.put("nom", req.nom);
	    params.put("prenom", req.prenom);
	    params.put("civilite", req.civilite);
	    params.put("titre_sujet", req.titreSujet);
	    params.put("profil", req.profil);
	    params.put("email", req.email);
	    params.put("motivation", req.message);
	    params.put("url_ressources", req.urlVitrine);
	    params.put("nom_plateforme", "Doctorat.gouv");
	    
	    // Récupération de l’URL de candidature
	    getSafeUrlCandidature(req.idPropositionThese).ifPresent(url -> params.put("url_candidature", url));

	    List<Attachment> attachments = new ArrayList<>();

	    if (req.cvBase64 != null && !req.cvBase64.isBlank()) {
	        attachments.add(new Attachment("cv.pdf", req.cvBase64));
	    }

	    if (req.documentBase64 != null && !req.documentBase64.isBlank()) {
	        attachments.add(new Attachment("autre_document.pdf", req.documentBase64));
	    }

	    try {
	    	if (isValidEmail(req.emailEncadrant)) {
	    		int templateId = resolveTemplateIdEncadrant(req.getTypeOffre());
	    		log.info("Envoi de l'email à l'encadrant avec template {}", templateId);
	    	    emailService.sendTemplateEmailWithAttachments(req.emailEncadrant, templateId, params, attachments);
	    	}
	    } catch (JsonProcessingException e) {
	        log.error("Error sending template email to encadrant", e);
	    }

	    log.info("Mail encadrant envoyé");
	}

	
	/**
	 * Prépare et envoie un e‑mail au candidat en utilisant un template.
	 * @param request
	 */
	private void templateMailCandidat(ContactRequest request) {
		log.info("Préparation template mail candidat");
		
		Map<String, Object> params = new HashMap<>();
		params.put("nom", request.nom);
		params.put("prenom", request.prenom);
		params.put("titre_sujet", request.titreSujet);
		params.put("email", request.email);
		params.put("motivation", request.message);
		params.put("nom_plateforme", "DOCTORAT GOUV");
		String url = String.format("https://app.doctorat.gouv.fr/proposition?id=%s", request.getIdPropositionThese());
		params.put("url_ressources", url);
		
        
        try {
			if (isValidEmail(request.email)) {
				int templateId = resolveTemplateIdCandidat(request.profil); 
				emailService.sendTemplateEmail(request.email, templateId, params);
			}
		} catch (JsonProcessingException e) {
			log.error("Error sending template email to candidate", e);
		}
        log.info("Mail candidat envoyé");
	}
	
	/**
	 * Récupère l'URL de candidature associée à une proposition de thèse.
	 * Retourne Optional.empty() si l'ID est invalide, si la proposition n'existe pas,
	 * si l'URL est absente ou en cas d'erreur.
	 */
	private Optional<String> getSafeUrlCandidature(long idPropositionThese) {

	    if (idPropositionThese <= 0) {
	        log.warn("ID PropositionThese non fourni ou égal à 0");
	        return Optional.empty();
	    }

	    try {
	        PropositionTheseDto dto = propositionTheseService.findById(idPropositionThese);

	        if (dto != null && dto.getUrlCandidature() != null && !dto.getUrlCandidature().isBlank()) {
	            log.info("URL de candidature trouvée pour la proposition {}", idPropositionThese);
	            return Optional.of(dto.getUrlCandidature());
	        } else {
	            log.warn("PropositionThese trouvée mais URL de candidature absente pour ID {}", idPropositionThese);
	            return Optional.empty();
	        }

	    } catch (Exception e) {
	        log.error("Erreur lors de la récupération de la proposition ID {}", idPropositionThese, e);
	        return Optional.empty();
	    }
	}
	
	/**
	 * Valide les données de la requête de contact.
	 * @param req
	 */
	private void validateRequest(ContactRequest req) {

	    if (req.getIdPropositionThese() <= 0) {
	        throw new IllegalArgumentException("ID de proposition invalide.");
	    }

	    if (req.getEmailEncadrant() == null || !isValidEmail(req.getEmailEncadrant())) {
	        throw new IllegalArgumentException("Email encadrant invalide.");
	    }

	    if (req.getEmail() == null || !isValidEmail(req.getEmail())) {
	        throw new IllegalArgumentException("Email candidat invalide.");
	    }

	}

	
	/**
	 * Résout l'ID du template à utiliser pour le candidat en fonction de son profil.
	 * @param profil
	 * @return
	 */
	private int resolveTemplateIdCandidat(String profil) {
	    return TEMPLATE_BY_PROFIL.getOrDefault(profil, 14); // 14 = valeur par défaut
	}
	
	/**
	 * Résout l'ID du template à utiliser pour l'encadrant en fonction du type d'offre.
	 * @param typeOffre
	 * @return
	 */
	private int resolveTemplateIdEncadrant(String typeOffre) {
	    return TEMPLATE_BY_TYPE_OFFRE.getOrDefault(typeOffre, 24); // 24 = valeur par défaut
	}

	
	/**
	 * Valide le format de l'adresse e‑mail.
	 * @param email
	 * @return
	 */
	private boolean isValidEmail(String email) {
	    return email != null
	        && !email.isBlank()
	        && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
	}

}
