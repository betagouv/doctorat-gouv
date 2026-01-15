package fr.dinum.beta.gouv.doctorat.controller;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

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
    public ResponseEntity<String> sendMails(@RequestBody ContactRequest request) {
    	log.info("Demande de contact reçue le : {}", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(java.time.LocalDateTime.now()));
        templateMailEncadrant(request);
        templateMailCandidat(request);
        return ResponseEntity.ok("Email envoyé");
    }

    
	/**
	 * Prépare et envoie un e‑mail au encadrant en utilisant un template.
	 * @param request
	 */
	private void templateMailEncadrant(ContactRequest request) {
		log.info("Préparation template mail encadrant pour : {}", request.emailEncadrant);
		Map<String, Object> params = Map.of(
				"nom", request.nom,
			    "prenom", request.prenom,
			    "civilite", request.civilite,
			    "titre_sujet", "Sujet de thèse intelligence artificielle",
			    "email", request.email,
			    "motivation", request.message,
			    "url_ressources", "https://doctorat-gouv.fr",
			    "nom_plateforme", "DOCTORAT GOUV"
			);

        
        try {
        	emailService.sendTemplateEmail(request.emailEncadrant, 15, params);
		} catch (JsonProcessingException e) {
			log.error("Error sending template email to candidate: {}", request.email, e);
		}
        log.info("Mail encadrant envoyé");
	}
	
	/**
	 * Prépare et envoie un e‑mail au candidat en utilisant un template.
	 * @param request
	 */
	private void templateMailCandidat(ContactRequest request) {
		log.info("Préparation template mail candidat pour : {}", request.email);
		Map<String, Object> params = Map.of(
				"nom", request.nom,
			    "prenom", request.prenom,
			    "titre_sujet", "Sujet de thèse intelligence artificielle",
			    "email", request.email,
			    "motivation", request.message,
			    "url_ressources", "https://doctorat-gouv.fr",
			    "nom_plateforme", "DOCTORAT GOUV"
			);

        
        try {
        	emailService.sendTemplateEmail(request.email,14, params);
		} catch (JsonProcessingException e) {
			log.error("Error sending template email to candidate: {}", request.email, e);
		}
        log.info("Mail candidat envoyé");
	}

}
