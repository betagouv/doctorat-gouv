package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.dinum.beta.gouv.doctorat.dto.FiltreRechercheRequest;
import fr.dinum.beta.gouv.doctorat.dto.FiltreRechercheResponse;
import fr.dinum.beta.gouv.doctorat.entity.FiltreRechercheCandidat;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.repository.FiltreRechercheCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@Service
public class FiltreRechercheService {

    private static final Logger log = LoggerFactory.getLogger(FiltreRechercheService.class);

    private final FiltreRechercheCandidatRepository repository;
    private final UtilisateurRepository utilisateurRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FiltreRechercheService(FiltreRechercheCandidatRepository repository,
                                 UtilisateurRepository utilisateurRepository) {
        this.repository = repository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public List<FiltreRechercheResponse> lister(String userId) {
        Utilisateur utilisateur = getUtilisateur(userId);
        ensureCandidat(utilisateur);
        return repository.findByUtilisateurIdOrderByDateCreationDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    public FiltreRechercheResponse creer(String userId, FiltreRechercheRequest request) {
        Utilisateur utilisateur = getUtilisateur(userId);
        ensureCandidat(utilisateur);

        String json;
        try {
            json = objectMapper.writeValueAsString(request.getFiltres());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Filtres invalides", e);
        }

        FiltreRechercheCandidat entity = FiltreRechercheCandidat.builder()
            .utilisateur(utilisateur)
            .nom(request.getNom().trim())
            .filtresJson(json)
            .build();

        FiltreRechercheCandidat saved = repository.save(entity);
        log.info("Filtre de recherche enregistre (id={}) pour candidat {}", saved.getId(), userId);
        return toResponse(saved);
    }

    public FiltreRechercheResponse mettreAJour(String userId, String id, FiltreRechercheRequest request) {
        FiltreRechercheCandidat entity = repository.findByIdAndUtilisateurId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Filtre introuvable"));

        String json;
        try {
            json = objectMapper.writeValueAsString(request.getFiltres());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Filtres invalides", e);
        }

        entity.setNom(request.getNom().trim());
        entity.setFiltresJson(json);
        entity.setDerniereAlerteEnvoyeeLe(LocalDateTime.now());

        FiltreRechercheCandidat saved = repository.save(entity);
        log.info("Filtre de recherche mis a jour (id={}) pour candidat {}", saved.getId(), userId);
        return toResponse(saved);
    }

    public void supprimer(String userId, String id) {
        FiltreRechercheCandidat entity = repository.findByIdAndUtilisateurId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Filtre introuvable"));
        repository.delete(entity);
        repository.flush();
        log.info("Filtre de recherche supprime (id={}) pour candidat {}", id, userId);
    }

    public FiltreRechercheResponse activerAlerte(String userId, String id, boolean active, String frequence) {
        FiltreRechercheCandidat entity = repository.findByIdAndUtilisateurId(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("Filtre introuvable pour id=" + id + " utilisateur=" + userId));
        entity.setAlerteActive(active);
        if (active) {
            entity.setFrequenceAlerte(frequence != null ? frequence : "HEBDOMADAIRE");
        } else {
            entity.setFrequenceAlerte(null);
        }
        FiltreRechercheCandidat saved = repository.save(entity);
        return toResponse(saved);
    }

    private Utilisateur getUtilisateur(String userId) {
        return utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    private void ensureCandidat(Utilisateur utilisateur) {
        if (utilisateur.getRole() != RoleUtilisateur.CANDIDAT) {
            throw new SecurityException("Acces reserve aux candidats");
        }
    }

    private FiltreRechercheResponse toResponse(FiltreRechercheCandidat entity) {
        Object filtres;
        try {
            filtres = objectMapper.readValue(entity.getFiltresJson(), Object.class);
        } catch (JsonProcessingException e) {
            filtres = entity.getFiltresJson();
        }
        return FiltreRechercheResponse.builder()
            .id(entity.getId())
            .nom(entity.getNom())
            .filtres(filtres)
            .alerteActive(entity.isAlerteActive())
            .frequenceAlerte(entity.getFrequenceAlerte())
            .derniereAlerteEnvoyeeLe(entity.getDerniereAlerteEnvoyeeLe())
            .dateCreation(entity.getDateCreation())
            .build();
    }
}
