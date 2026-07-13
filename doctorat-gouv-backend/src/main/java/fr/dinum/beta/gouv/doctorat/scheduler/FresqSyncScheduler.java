package fr.dinum.beta.gouv.doctorat.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.entity.EcoleDoctorale;
import fr.dinum.beta.gouv.doctorat.model.FresqSearchContent;
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
			List<FresqSearchContent> ecolesFresq = fresqApiService.recupererToutesLesEcolesDoctorales();
			
			if (ecolesFresq == null || ecolesFresq.isEmpty()) {
				log.warn("Aucune école doctorale récupérée depuis Fresq");
				return;
			}
			
			log.info("Nombre d'écoles doctorales récupérées depuis Fresq: {}", ecolesFresq.size());
			
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
			
			// Traiter chaque école doctorale de Fresq
			for (FresqSearchContent contenu : ecolesFresq) {
				Map<String, Object> data = contenu.getData();
				if (data == null) {
					log.debug("École doctorale ignorée (data null): recordId={}", contenu.getRecordId());
					continue;
				}
				
				String numero = (String) data.get("numero_ed");
				String libelle = (String) data.get("libelle_ed");
				
				if (numero == null || libelle == null) {
					log.debug("École doctorale ignorée (numéro ou libellé manquant): {}", data);
					continue;
				}
				
				Optional<EcoleDoctorale> existanteOpt = ecoleDoctoraleRepository.findByNumero(numero);
				
				if (existanteOpt.isPresent()) {
					// Mise à jour
					EcoleDoctorale existante = existanteOpt.get();
					log.info("Mise à jour école doctorale {}: {} -> {}", numero, existante.getLibelle(), libelle);
					existante.setLibelle(libelle);
					existante.setFresqRecordId(contenu.getRecordId());
					existante.setDateSynchronisation(LocalDateTime.now());
					existante.setActive(true);
					
					// Mettre à jour d'autres champs si disponibles
					if (data.get("uai") != null) {
						existante.setUai((String) data.get("uai"));
					}
					if (data.get("academie") != null) {
						existante.setAcademie((String) data.get("academie"));
					}
					if (data.get("region_academie") != null) {
						existante.setRegionAcademie((String) data.get("region_academie"));
					}
					if (data.get("secteur") != null) {
						existante.setSecteur((String) data.get("secteur"));
					}
					if (data.get("specialite") != null) {
						existante.setSpecialite((String) data.get("specialite"));
					}
					if (data.get("etablissement_ror") != null) {
						existante.setEtablissementRor((String) data.get("etablissement_ror"));
					}
					if (data.get("etablissement_libelle") != null) {
						existante.setEtablissementLibelle((String) data.get("etablissement_libelle"));
					}
					
					nbMisAJour++;
				} else {
					// Nouvelle création
					log.info("Nouvelle école doctorale créée: {} - {}", numero, libelle);
					EcoleDoctorale nouvelle = new EcoleDoctorale();
					nouvelle.setNumero(numero);
					nouvelle.setLibelle(libelle);
					nouvelle.setFresqRecordId(contenu.getRecordId());
					nouvelle.setDateSynchronisation(LocalDateTime.now());
					nouvelle.setActive(true);
					nouvelle.setUai((String) data.get("uai"));
					nouvelle.setAcademie((String) data.get("academie"));
					nouvelle.setRegionAcademie((String) data.get("region_academie"));
					nouvelle.setSecteur((String) data.get("secteur"));
					nouvelle.setSpecialite((String) data.get("specialite"));
					nouvelle.setEtablissementRor((String) data.get("etablissement_ror"));
					nouvelle.setEtablissementLibelle((String) data.get("etablissement_libelle"));
					
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
