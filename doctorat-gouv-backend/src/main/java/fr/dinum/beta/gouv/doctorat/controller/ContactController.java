package fr.dinum.beta.gouv.doctorat.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import fr.dinum.beta.gouv.doctorat.service.BrevoEmailService;

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

    public ContactController(BrevoEmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Méthode POST pour envoyer des e‑mails à partir des demandes de contact. 
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<?> sendMails(@RequestBody ContactRequest request) {
    	log.info("Réception d'une demande de contact le : " + java.time.LocalDateTime.now());
    	
    	if (mailEnabled) {
    	    templateMailEncadrant(request);
    	    templateMailCandidat(request);
    	} else {
    	    log.warn("Mode DEV : mails désactivés");
    	}

        return ResponseEntity.ok(Map.of("message", "Email envoyé"));
    }


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
	    		log.info("Envoi de l'email à l'encadrant : {} avec template {}", req.emailEncadrant, templateId);
	    	    emailService.sendTemplateEmailWithAttachments(req.emailEncadrant, templateId, params, attachments);
	    	}
	    } catch (JsonProcessingException e) {
	        log.error("Error sending template email to encadrant: {}", req.emailEncadrant, e);
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
			log.error("Error sending template email to candidate: {}", request.email, e);
		}
        log.info("Mail candidat envoyé");
	}
	
	private int resolveTemplateIdCandidat(String profil) {
	    return TEMPLATE_BY_PROFIL.getOrDefault(profil, 14); // 14 = valeur par défaut
	}
	
	private int resolveTemplateIdEncadrant(String typeOffre) {
	    return TEMPLATE_BY_TYPE_OFFRE.getOrDefault(typeOffre, 24); // 24 = valeur par défaut
	}

	
	private boolean isValidEmail(String email) {
	    return email != null
	        && !email.isBlank()
	        && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
	}

}
