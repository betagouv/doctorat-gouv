package fr.dinum.beta.gouv.doctorat.controller;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        templateMailEncadrant(request);
        templateMailCandidat(request);
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
	    	    emailService.sendTemplateEmailWithAttachments(req.emailEncadrant, 15, params, attachments);
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
		params.put("titre_sujet", "Sujet de thèse intelligence artificielle");
		params.put("email", request.email);
		params.put("motivation", request.message);
		params.put("url_ressources", "https://doctorat.sites.beta.gouv.fr/");
		params.put("nom_plateforme", "DOCTORAT GOUV");
        
        try {
			if (isValidEmail(request.email)) {
				emailService.sendTemplateEmail(request.email, 14, params);
			}
		} catch (JsonProcessingException e) {
			log.error("Error sending template email to candidate: {}", request.email, e);
		}
        log.info("Mail candidat envoyé");
	}
	
	private boolean isValidEmail(String email) {
	    return email != null
	        && !email.isBlank()
	        && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
	}

}
