package fr.dinum.beta.gouv.doctorat.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.entity.EcoleDoctorale;
import fr.dinum.beta.gouv.doctorat.model.FresqEtablissement;
import fr.dinum.beta.gouv.doctorat.repository.EcoleDoctoraleRepository;
import fr.dinum.beta.gouv.doctorat.service.FresqApiService;

@Component
public class FresqSyncScheduler {
	
	private static final Logger log = LoggerFactory.getLogger(FresqSyncScheduler.class);
	
	@Value("${fresq.scheduler.cron}")
	private String cronExpression;
	
	private final FresqApiService fresqApiService;
	private final EcoleDoctoraleRepository ecoleDoctoraleRepository;
	
	public FresqSyncScheduler(FresqApiService fresqApiService, EcoleDoctoraleRepository ecoleDoctoraleRepository) {
		this.fresqApiService = fresqApiService;
		this.ecoleDoctoraleRepository = ecoleDoctoraleRepository;
	}
	
	@Scheduled(cron = "${fresq.scheduler.cron}")
	public void synchroniserEcolesDoctorales() {
		log.info("Début de la synchronisation des écoles doctorales depuis Fresq");
		
		try {
			List<FresqEtablissement> etablissementsFresq = fresqApiService.recupererTousLesEtablissements();
			
			if (etablissementsFresq == null || etablissementsFresq.isEmpty()) {
				log.warn("Aucun établissement récupéré depuis Fresq");
				return;
			}
			
			log.info("Nombre d'établissements récupérés depuis Fresq: {}", etablissementsFresq.size());
			
			int nbAjoutes = 0;
			int nbMisAJour = 0;
			int nbDesactives = 0;
			
			// Marquer toutes les écoles existantes comme non synchronisées
			List<EcoleDoctorale> ecolesExistantes = ecoleDoctoraleRepository.findAll();
			log.info("Nombre d'écoles doctorales existantes en base: {}", ecolesExistantes.size());
			for (EcoleDoctorale ed : ecolesExistantes) {
				ed.setActive(false);
			}
			ecoleDoctoraleRepository.saveAll(ecolesExistantes);
			
			// Traiter chaque établissement de Fresq
			for (FresqEtablissement etablissement : etablissementsFresq) {
				String uai = etablissement.getUai();
				String nom = etablissement.getNom();
				
				if (uai == null || nom == null) {
					log.debug("Établissement ignoré (uai ou nom null): uai={}, nom={}", uai, nom);
					continue;
				}
				
				// Chercher si l'école doctorale existe déjà par UAI
				List<EcoleDoctorale> existantes = ecoleDoctoraleRepository.findByUai(uai);
				Optional<EcoleDoctorale> existanteOpt = existantes.isEmpty() ? Optional.empty() : Optional.of(existantes.get(0));
				
				if (existanteOpt.isPresent()) {
					// Mise à jour
					EcoleDoctorale existante = existanteOpt.get();
					log.info("Mise à jour école doctorale UAI {}: {} -> {}", uai, existante.getLibelle(), nom);
					existante.setLibelle(nom);
					existante.setDateSynchronisation(LocalDateTime.now());
					existante.setActive(true);
					
					if (etablissement.getSigle() != null) {
						existante.setEtablissementLibelle(etablissement.getSigle());
					}
					
					nbMisAJour++;
				} else {
					// Nouvelle création
					log.info("Nouvelle école doctorale créée: UAI {} - {}", uai, nom);
					EcoleDoctorale nouvelle = new EcoleDoctorale();
					nouvelle.setNumero(uai.length() > 3 ? uai.substring(0, 3) : uai);
					nouvelle.setLibelle(nom);
					nouvelle.setUai(uai);
					nouvelle.setDateSynchronisation(LocalDateTime.now());
					nouvelle.setActive(true);
					
					if (etablissement.getSigle() != null) {
						nouvelle.setEtablissementLibelle(etablissement.getSigle());
					}
					
					ecoleDoctoraleRepository.save(nouvelle);
					nbAjoutes++;
				}
			}
			
			// Désactiver les écoles qui n'ont pas été synchronisées
			List<EcoleDoctorale> ecolesDesactiver = ecoleDoctoraleRepository.findAll().stream()
					.filter(ed -> !ed.getActive())
					.toList();
			
			log.info("Nombre d'écoles doctorales à désactiver: {}", ecolesDesactiver.size());
			for (EcoleDoctorale ed : ecolesDesactiver) {
				log.info("Désactivation école doctorale {}: {}", ed.getNumero(), ed.getLibelle());
				ed.setActive(false);
				nbDesactives++;
			}
			ecoleDoctoraleRepository.saveAll(ecolesDesactiver);
			
			log.info("Synchronisation terminée: {} ajoutées, {} mises à jour, {} désactivées", 
					nbAjoutes, nbMisAJour, nbDesactives);
			
		} catch (Exception e) {
			log.error("Erreur lors de la synchronisation des écoles doctorales: {}", e.getMessage(), e);
		}
	}
}
