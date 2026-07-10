package fr.dinum.beta.gouv.doctorat.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

@RestController
public class SitemapController {

	private final PropositionTheseRepository repository;

	public SitemapController(PropositionTheseRepository repository) {
		this.repository = repository;
	}

	@GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
	public String sitemap() {
		List<PropositionThese> theses = repository.findActivePropositions();
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String today = LocalDate.now().format(fmt);

		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

		urlEntry(sb, "https://app.doctorat.gouv.fr/", today, "daily", "1.0");
		urlEntry(sb, "https://app.doctorat.gouv.fr/search", today, "daily", "0.9");
		urlEntry(sb, "https://app.doctorat.gouv.fr/contact", today, "monthly", "0.7");

		for (PropositionThese t : theses) {
			String lastmod = t.getDateMiseEnLigne() != null
				? t.getDateMiseEnLigne().format(fmt)
				: today;
			urlEntry(sb,
				"https://app.doctorat.gouv.fr/proposition?id=" + t.getId(),
				lastmod,
				"weekly",
				"0.8"
			);
		}

		sb.append("</urlset>");
		return sb.toString();
	}

	private void urlEntry(StringBuilder sb, String loc, String lastmod,
						  String changefreq, String priority) {
		sb.append("<url>");
		sb.append("<loc>").append(escapeXml(loc)).append("</loc>");
		sb.append("<lastmod>").append(lastmod).append("</lastmod>");
		sb.append("<changefreq>").append(changefreq).append("</changefreq>");
		sb.append("<priority>").append(priority).append("</priority>");
		sb.append("</url>");
	}

	private String escapeXml(String s) {
		return s.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}
}
