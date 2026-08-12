package fr.dinum.beta.gouv.doctorat.service;

import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.config.EuraxessProperties;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.JobOpportunities;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.JobOpportunity;
import fr.dinum.beta.gouv.doctorat.mapper.EuraxessFeedMapper;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

@Service
public class EuraxessFeedService {

	private static final Logger log = LoggerFactory.getLogger(EuraxessFeedService.class);

	private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	private final PropositionTheseRepository propositionTheseRepository;
	private final EuraxessFeedMapper mapper;
	private final EuraxessProperties properties;
	private final Marshaller marshaller;

	public EuraxessFeedService(PropositionTheseRepository propositionTheseRepository,
			EuraxessFeedMapper mapper, EuraxessProperties properties) {
		this.propositionTheseRepository = propositionTheseRepository;
		this.mapper = mapper;
		this.properties = properties;
		this.marshaller = createMarshaller();
	}

	/**
	 * Génère le flux XML EURAXESS (racine {@code job-opportunities}) à partir
	 * des propositions de thèse actives.
	 */
	public String generateFeed() {
		long start = System.currentTimeMillis();
		JobOpportunities root = buildRoot();
		String xml = marshal(root);
		log.info("Flux EURAXESS généré : {} offres, {} ms",
				root.getJobOpportunity() == null ? 0 : root.getJobOpportunity().size(),
				System.currentTimeMillis() - start);
		return xml;
	}

	private JobOpportunities buildRoot() {
		JobOpportunities root = new JobOpportunities();
		root.setIsIncremental(false);
		root.setDatasourceKey(properties.getDatasourceKey());

		var propositions = propositionTheseRepository.findByActiveTrue(
				PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "dateMaj"))).getContent();

		int excluded = 0;
		for (PropositionThese p : propositions) {
			JobOpportunity job = mapper.toJobOpportunity(p);
			if (job == null) {
				excluded++;
				log.warn("Offre exclue du flux EURAXESS (champ obligatoire manquant) : matricule={}",
						p.getMatricule());
			} else {
				root.getJobOpportunity().add(job);
			}
		}
		if (excluded > 0) {
			log.warn("Flux EURAXESS : {} offre(s) exclue(s) sur {}", excluded, propositions.size());
		}
		return root;
	}

	private String marshal(JobOpportunities root) {
		try {
			StringWriter writer = new StringWriter();
			marshaller.marshal(root, writer);
			return XML_DECLARATION + "\n" + writer;
		} catch (JAXBException e) {
			throw new IllegalStateException("Erreur lors du marshalling du flux EURAXESS", e);
		}
	}

	private static Marshaller createMarshaller() {
		try {
			Marshaller m = JAXBContext.newInstance(JobOpportunities.class).createMarshaller();
			m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			return m;
		} catch (JAXBException e) {
			throw new IllegalStateException("Erreur lors de l'initialisation du marshaller JAXB", e);
		}
	}

}
