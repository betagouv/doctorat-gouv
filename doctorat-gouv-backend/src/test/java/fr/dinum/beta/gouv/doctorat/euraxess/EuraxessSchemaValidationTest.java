package fr.dinum.beta.gouv.doctorat.euraxess;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import fr.dinum.beta.gouv.doctorat.config.EuraxessProperties;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.JobOpportunities;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.JobOpportunity;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

/**
 * Valide le flux XML EURAXESS généré contre le schéma officiel
 * {@code JobOpportunitySchema.xsd}.
 */
class EuraxessSchemaValidationTest {

	private static final String XSD = "/euraxess/JobOpportunitySchema.xsd";

	private Schema schema;
	private Marshaller marshaller;

	@BeforeEach
	void setUp() throws Exception {
		try (InputStream xsd = getClass().getResourceAsStream(XSD)) {
			assertNotNull(xsd, "Schéma EURAXESS introuvable sur le classpath");
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			schema = factory.newSchema(new StreamSource(xsd));
		}
		marshaller = JAXBContext.newInstance(JobOpportunities.class).createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	}

	private void assertValidAgainstXsd(JobOpportunities root) throws Exception {
		java.io.StringWriter writer = new java.io.StringWriter();
		marshaller.marshal(root, writer);
		Validator validator = schema.newValidator();
		validator.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException exception) {
			}

			@Override
			public void error(SAXParseException exception) throws SAXParseException {
				throw exception;
			}

			@Override
			public void fatalError(SAXParseException exception) throws SAXParseException {
				throw exception;
			}
		});
		try {
			validator.validate(new StreamSource(new StringReader(writer.toString())));
		} catch (SAXParseException e) {
			fail("Le flux généré n'est pas valide contre le XSD : " + e.getMessage()
					+ "\n--- XML ---\n" + writer);
		}
	}

	@Test
	void emptyFeedIsWellFormedButNotSchemaValid() throws Exception {
		JobOpportunities root = new JobOpportunities();
		root.setIsIncremental(true);
		root.setDatasourceKey("doctorat-gouv");
		String xml = marshal(root);

		// Bien formé (marshaller JAXB sain), mais non validable contre le XSD
		// officiel (minOccurs=1 sur job-opportunity) : c'est pour cela que
		// l'endpoint ne l'émet jamais et répond 204 No Content.
		assertTrue(isWellFormed(xml), "Le flux de secours doit être bien formé : " + xml);
	}

	private String marshal(JobOpportunities root) throws Exception {
		java.io.StringWriter writer = new java.io.StringWriter();
		marshaller.marshal(root, writer);
		return writer.toString();
	}

	private static boolean isWellFormed(String xml) {
		try {
			DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new org.xml.sax.InputSource(new StringReader(xml)));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Test
	void fullyMappedFeedIsValid() throws Exception {
		JobOpportunities root = new JobOpportunities();
		root.setIsIncremental(false);
		root.setDatasourceKey("doctorat-gouv");

		EuraxessProperties props = new EuraxessProperties();
		props.setOrganisationIdKey("doctorat-gouv-dev");
		props.setContactEmail("contact@doctorat.gouv.fr");

		var mapper = new fr.dinum.beta.gouv.doctorat.mapper.EuraxessFeedMapper(props);
		JobOpportunity job = mapper.toJobOpportunity(completeProposition());
		assertNotNull(job);
		root.getJobOpportunity().add(job);

		assertValidAgainstXsd(root);
	}

	private static fr.dinum.beta.gouv.doctorat.entity.PropositionThese completeProposition() {
		var p = new fr.dinum.beta.gouv.doctorat.entity.PropositionThese();
		p.setMatricule("AB-2026_0001");
		p.setTheseTitre("Titre de la thèse");
		p.setTheseTitreAnglais("Thesis title");
		p.setResume("Résumé court");
		p.setResumeAnglais("Short summary");
		p.setObjectif("Objectif");
		p.setDateMaj(java.time.LocalDateTime.of(2026, 8, 1, 12, 30, 0));
		p.setDateLimiteCandidature(java.time.LocalDateTime.of(2026, 10, 1, 0, 0, 0));
		p.setDomaineScientifique("2");
		p.setSpecialite("physique");
		p.setEtablissementLibelle("Université Exemple");
		p.setEtablissementVille("Paris");
		p.setEtablissementCodePostal("75000");
		p.setUniteRechercheLibelle("Laboratoire de Recherche");
		p.setEcoleDoctoraleLibelle("École Doctorale Exemple");
		p.setDeposantEmail("deposant@exemple.fr");
		p.setDirectionTheseEmail("directeur@exemple.fr");
		p.setNiveauAnglaisRequis("C1");
		p.setNiveauFrancaisRequis("C2");
		p.setUrlCandidature("https://exemple.fr/candidature");
		p.setUrlInfosComplementaires("https://exemple.fr/infos");
		p.setUrlPdf("https://exemple.fr/sujet.pdf");
		return p;
	}
}
