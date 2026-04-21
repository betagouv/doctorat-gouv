package fr.dinum.beta.gouv.doctorat.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Ce service est responsable de découper un texte en chunks de taille maximale définie, en évitant de couper au milieu d’un mot.
 */
@Service
public class TextChunker {
	
	private static final Logger log = LoggerFactory.getLogger(TextChunker.class);

	private static final int CHUNK_SIZE = 500;

	/**
	 * Découpe le texte en chunks de taille maximale CHUNK_SIZE
	 * @param text
	 * @return
	 */
	public List<String> chunk(String text) {
		
		log.info("Découpage du texte en chunks de taille maximale {} caractères", CHUNK_SIZE);
		
		if (text == null || text.isBlank()) {
			log.info("Le texte est vide ou null, aucun chunk à générer");
			return List.of();
		}

		text = text.trim();

		List<String> chunks = new ArrayList<>();

		int index = 0;
		while (index < text.length()) {
			int end = Math.min(index + CHUNK_SIZE, text.length());

			// éviter de couper au milieu d’un mot
			if (end < text.length()) {
				int lastSpace = text.lastIndexOf(" ", end);
				if (lastSpace > index) {
					end = lastSpace;
				}
			}

			String chunk = text.substring(index, end).trim();
			chunks.add(chunk);

			index = end;
		}

		return chunks;
	}
}
