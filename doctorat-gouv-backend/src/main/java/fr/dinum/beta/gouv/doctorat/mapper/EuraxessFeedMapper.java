package fr.dinum.beta.gouv.doctorat.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.config.EuraxessProperties;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.AdditionalRequirements;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.ApplicationDetails;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.Description;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.DisciplineEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.EuFunding;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.HiringOrgInst;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.HowToApplyEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.JobOpportunity;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.JobPositionsEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.JobStatusEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.LanguagesEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.LanguagesLevelEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.OrganisationTypeEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.RequiredLanguages;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.ResearchField;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.ResearchFrameworkProgrammeEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.ResearcherProfileEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.TypeOfContractEnum;
import fr.dinum.beta.gouv.doctorat.euraxess.jaxb.WorkLocation;

/**
 * Mapping champ à champ d'une {@link PropositionThese} vers le graphe JAXB
 * EURAXESS ({@link JobOpportunity}). Règles R3/R4/R5/R6/R7 du document de
 * mapping : replis documentés, exclusion si champ obligatoire manquant,
 * troncatures aux maxima du XSD, omission des éléments non mappés.
 */
@Component
public class EuraxessFeedMapper {

	private static final int MAX_SHORT = 255;
	private static final int MAX_MEDIUM = 512;
	private static final int MAX_LONG = 10_000;
	private static final int MAX_DESCRIPTION = 50_000;
	private static final int MAX_JOB_ID = 50;

	private static final DatatypeFactory DATATYPE_FACTORY = newDatatypeFactory();

	private static final Map<String, DisciplineEnum> DOMAIN_MAPPING = Map.of(
			"1", DisciplineEnum.MATHEMATICS,
			"2", DisciplineEnum.PHYSICS,
			"3", DisciplineEnum.GEOSCIENCES,
			"4", DisciplineEnum.CHEMISTRY,
			"5", DisciplineEnum.BIOLOGICAL_SCIENCES,
			"6", DisciplineEnum.CULTURAL_STUDIES,
			"7", DisciplineEnum.SOCIOLOGY,
			"8", DisciplineEnum.ENGINEERING,
			"9", DisciplineEnum.COMPUTER_SCIENCE,
			"10", DisciplineEnum.AGRICULTURAL_SCIENCES
	);

	private final EuraxessProperties properties;

	public EuraxessFeedMapper(EuraxessProperties properties) {
		this.properties = properties;
	}

	/**
	 * Construit le {@link JobOpportunity} EURAXESS à partir de la proposition.
	 * Retourne {@code null} si un champ obligatoire est absent après repli
	 * (règle R4 : l'offre est exclue du flux, le service journalise un WARN).
	 */
	public JobOpportunity toJobOpportunity(PropositionThese p) {
		if (isBlank(properties.getOrganisationIdKey())) {
			return null;
		}
		String jobId = cleanJobId(p.getMatricule());
		if (jobId == null) {
			return null;
		}
		String jobTitle = truncate(sanitizeXml(p.getTheseTitre()), MAX_SHORT);
		if (jobTitle == null) {
			return null;
		}
		String jobDescription = buildJobDescription(p);
		if (jobDescription == null) {
			return null;
		}
		XMLGregorianCalendar deadline = toXmlDateTime(p.getDateLimiteCandidature());
		if (deadline == null) {
			return null;
		}
		String organisationInstitute = truncate(sanitizeXml(p.getEtablissementLibelle()), MAX_MEDIUM);
		if (organisationInstitute == null) {
			return null;
		}
		String jobOrganisation = truncate(sanitizeXml(firstNonBlank(p.getUniteRechercheLibelle(), p.getEtablissementLibelle())), MAX_MEDIUM);
		if (jobOrganisation == null) {
			return null;
		}

		JobOpportunity job = new JobOpportunity();
		job.setOrganisationIDKey(properties.getOrganisationIdKey());
		XMLGregorianCalendar lastModified = toXmlDateTime(p.getDateMaj());
		if (lastModified == null) {
			lastModified = toXmlDateTime(java.time.LocalDateTime.now());
		}
		job.setLastmodifieddate(lastModified);
		job.setJobId(jobId);
		job.setDescription(buildDescription(p, jobTitle, jobDescription, deadline));
		job.setEuFunding(buildEuFunding());
		job.getWorkLocation().add(buildWorkLocation(p, jobOrganisation));
		job.setHiringOrgInst(buildHiringOrgInst(p, organisationInstitute));
		job.setApplicationDetails(buildApplicationDetails(p));
		job.setAdditionalRequirements(buildAdditionalRequirements(p));
		return job;
	}

	private Description buildDescription(PropositionThese p, String jobTitle, String jobDescription,
			XMLGregorianCalendar deadline) {
		Description d = new Description();
		d.setJobTitle(jobTitle);
		d.setJobDescription(jobDescription);
		d.setJobSummary(truncate(sanitizeXml(firstNonBlank(p.getResumeAnglais(), p.getTheseTitreAnglais())), MAX_LONG));
		d.setApplicationDeadline(deadline);
		d.getResearcherProfile().add(ResearcherProfileEnum.FIRST_STAGE_RESEARCHER_R_1);
		d.setTypeOfContract(TypeOfContractEnum.TEMPORARY);
		d.setJobStatus(JobStatusEnum.FULL_TIME);
		d.setPositions(JobPositionsEnum.PH_D_POSITIONS);

		ResearchField researchField = new ResearchField();
		researchField.setMainResearchField(mapDomain(p.getDomaineScientifique()));
		String subField = ResearchFieldDictionary.map(p.getSpecialite());
		if (subField != null) {
			researchField.setSubResearchField(subField);
		}
		d.getResearchField().add(researchField);
		return d;
	}

	private EuFunding buildEuFunding() {
		EuFunding f = new EuFunding();
		f.setFrameworkProgramme(ResearchFrameworkProgrammeEnum.NO);
		return f;
	}

	private WorkLocation buildWorkLocation(PropositionThese p, String jobOrganisation) {
		WorkLocation w = new WorkLocation();
		w.setNrJobPositions(1);
		w.setJobOrganisationInstitute(jobOrganisation);
		w.setJobCountry("France");
		w.setJobCity(truncate(sanitizeXml(p.getEtablissementVille()), MAX_MEDIUM));
		w.setJobPostalCode(truncate(sanitizeXml(p.getEtablissementCodePostal()), MAX_MEDIUM));
		return w;
	}

	private HiringOrgInst buildHiringOrgInst(PropositionThese p, String organisationInstitute) {
		HiringOrgInst h = new HiringOrgInst();
		h.setOrganisationInstitute(organisationInstitute);
		h.setOrganisationInstituteType(OrganisationTypeEnum.HIGHER_EDUCATION_INSTITUTE);
		h.setCountry("France");
		h.setDivisionFaculty(truncate(sanitizeXml(p.getEcoleDoctoraleLibelle()), MAX_MEDIUM));
		h.setCity(truncate(sanitizeXml(p.getEtablissementVille()), MAX_MEDIUM));
		h.setPostalCode(truncate(sanitizeXml(p.getEtablissementCodePostal()), MAX_MEDIUM));
		String email = firstNonBlank(p.getDeposantEmail(), properties.getContactEmail());
		if (email != null) {
			h.getEMail().add(truncate(email.trim(), MAX_MEDIUM));
		}
		String website = firstNonBlank(p.getUrlInfosComplementaires(), p.getUrlPdf());
		if (website != null) {
			h.getWebsite().add(truncate(website.trim(), MAX_MEDIUM));
		}
		return h;
	}

	private ApplicationDetails buildApplicationDetails(PropositionThese p) {
		ApplicationDetails a = new ApplicationDetails();
		String website = firstNonBlank(p.getUrlCandidature(), p.getUrlInfosComplementaires());
		if (website != null) {
			a.setHowToApply(HowToApplyEnum.WEBSITE);
			a.setApplicationWebsite(truncate(website.trim(), MAX_LONG));
		} else {
			a.setHowToApply(HowToApplyEnum.E_MAIL);
			String email = firstNonBlank(p.getDeposantEmail(), p.getDirectionTheseEmail());
			if (email != null) {
				a.setApplicationEmail(truncate(email.trim(), MAX_MEDIUM));
			}
		}
		return a;
	}

	private AdditionalRequirements buildAdditionalRequirements(PropositionThese p) {
		List<RequiredLanguages> languages = new ArrayList<>();
		LanguagesLevelEnum englishLevel = mapLanguageLevel(p.getNiveauAnglaisRequis());
		if (englishLevel != null) {
			RequiredLanguages english = new RequiredLanguages();
			english.setLanguage(LanguagesEnum.ENGLISH);
			english.setLanguageLevel(englishLevel);
			languages.add(english);
		}
		LanguagesLevelEnum frenchLevel = mapLanguageLevel(p.getNiveauFrancaisRequis());
		if (frenchLevel != null) {
			RequiredLanguages french = new RequiredLanguages();
			french.setLanguage(LanguagesEnum.FRENCH);
			french.setLanguageLevel(frenchLevel);
			languages.add(french);
		}
		if (languages.isEmpty()) {
			return null;
		}
		AdditionalRequirements req = new AdditionalRequirements();
		req.getRequiredLanguages().addAll(languages);
		return req;
	}

	private static DisciplineEnum mapDomain(String code) {
		if (code == null) {
			return DisciplineEnum.OTHER;
		}
		return DOMAIN_MAPPING.getOrDefault(code.trim(), DisciplineEnum.OTHER);
	}

	/**
	 * Mapping CEFR → languagesLevelEnum (validé MOA). "Aucun"/vide → {@code null}
	 * (règle R7 : langue omise).
	 */
	static LanguagesLevelEnum mapLanguageLevel(String level) {
		if (level == null || level.isBlank() || "aucun".equalsIgnoreCase(level.trim())) {
			return null;
		}
		return switch (level.trim().toUpperCase()) {
			case "C2" -> LanguagesLevelEnum.MOTHER_TONGUE;
			case "C1" -> LanguagesLevelEnum.EXCELLENT;
			case "B2" -> LanguagesLevelEnum.GOOD;
			case "B1", "A1", "A2" -> LanguagesLevelEnum.BASIC;
			default -> null;
		};
	}

	private static String buildJobDescription(PropositionThese p) {
		StringBuilder sb = new StringBuilder();
		appendSection(sb, p.getResume());
		appendSection(sb, p.getObjectif());
		appendSection(sb, p.getContexte());
		appendSection(sb, p.getMethodeDeTravail());
		appendSection(sb, p.getResultatsAttendus());
		String desc = truncate(sanitizeXml(sb.toString()), MAX_DESCRIPTION);
		if (desc == null) {
			return null;
		}
		return desc;
	}

	private static void appendSection(StringBuilder sb, String section) {
		if (section != null && !section.isBlank()) {
			if (sb.length() > 0) {
				sb.append("\n\n");
			}
			sb.append(section.trim());
		}
	}

	private static XMLGregorianCalendar toXmlDateTime(java.time.LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return DATATYPE_FACTORY.newXMLGregorianCalendar(value.toString());
	}

	private static DatatypeFactory newDatatypeFactory() {
		try {
			return DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new IllegalStateException("DatatypeFactory indisponible", e);
		}
	}

	/**
	 * Nettoie le matricule selon le pattern XSD textInputID
	 * ({@code [A-Za-z\d\s(-_)]*}, max 50). {@code null} si vide après nettoyage.
	 */
	static String cleanJobId(String matricule) {
		if (matricule == null) {
			return null;
		}
		String cleaned = matricule.replaceAll("[^A-Za-z0-9\\s()_-]", "");
		if (cleaned.isBlank()) {
			return null;
		}
		return cleaned.length() <= MAX_JOB_ID ? cleaned : cleaned.substring(0, MAX_JOB_ID);
	}

	static String truncate(String value, int max) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
	}

	/**
	 * Retire les caractères de contrôle invalides en XML 1.0 (sauf tabulation,
	 * retour ligne, retour chariot).
	 */
	static String sanitizeXml(String value) {
		if (value == null) {
			return null;
		}
		return value.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", "");
	}

	private static String firstNonBlank(String... values) {
		for (String v : values) {
			if (v != null && !v.isBlank()) {
				return v.trim();
			}
		}
		return null;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
