package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import fr.dinum.beta.gouv.doctorat.dto.ChangementMotDePasseRequest;
import fr.dinum.beta.gouv.doctorat.dto.FichierDto;
import fr.dinum.beta.gouv.doctorat.dto.ProfilResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilUpdateRequest;
import fr.dinum.beta.gouv.doctorat.entity.ProfilCandidat;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.repository.ProfilCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.service.storage.StoragePort;
import fr.dinum.beta.gouv.doctorat.util.EmailUtils;

@Service
public class ProfilService {

    private static final Logger log = LoggerFactory.getLogger(ProfilService.class);

    private final UtilisateurRepository utilisateurRepository;
    private final ProfilCandidatRepository profilCandidatRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoragePort storage;
    private final InscriptionFileService inscriptionFileService;

    public ProfilService(UtilisateurRepository utilisateurRepository,
                         ProfilCandidatRepository profilCandidatRepository,
                         PasswordEncoder passwordEncoder,
                         StoragePort storage,
                         InscriptionFileService inscriptionFileService) {
        this.utilisateurRepository = utilisateurRepository;
        this.profilCandidatRepository = profilCandidatRepository;
        this.passwordEncoder = passwordEncoder;
        this.storage = storage;
        this.inscriptionFileService = inscriptionFileService;
    }

    public Optional<ProfilResponse> getProfil(String userId) {
        return utilisateurRepository.findById(userId).map(this::toProfilResponse);
    }

    public ProfilResponse updateProfil(String userId, ProfilUpdateRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        String normalizedEmail = EmailUtils.normalize(request.getEmail());
        if (normalizedEmail != null && !normalizedEmail.equals(utilisateur.getEmail())) {
            utilisateurRepository.findByEmailIgnoreCaseAndTrim(normalizedEmail)
                .filter(u -> !u.getId().equals(userId))
                .ifPresent(u -> { throw new IllegalArgumentException("Un compte existe déjà avec cet email"); });
        }
        utilisateur.setEmail(normalizedEmail);
        if (request.getCompetences() != null) {
            utilisateur.setCompetences(new ArrayList<>(request.getCompetences()));
        }
        utilisateur.setDateModification(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        ProfilCandidat profil = profilCandidatRepository.findByUtilisateurId(userId)
            .orElse(new ProfilCandidat());
        if (profil.getUtilisateur() == null) {
            profil.setUtilisateur(utilisateur);
        }
        profil.setCivilite(request.getCivilite());
        profil.setSituation(request.getSituation());
        profil.setTelephone(request.getTelephone());
        profil.setOrcid(request.getOrcid());
        profil.setEtablissement(request.getEtablissement());
        profil.setLaboratoire(request.getLaboratoire());
        profilCandidatRepository.save(profil);

        log.info("Profil mis à jour pour l'utilisateur {}", userId);
        return toProfilResponse(utilisateur);
    }

    public ProfilResponse addCompetence(String userId, String competence) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (utilisateur.getCompetences() == null) {
            utilisateur.setCompetences(new ArrayList<>());
        }
        if (!utilisateur.getCompetences().contains(competence)) {
            utilisateur.getCompetences().add(competence);
            utilisateur.setDateModification(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);
            log.info("Compétence '{}' ajoutée pour l'utilisateur {}", competence, userId);
        }
        return toProfilResponse(utilisateur);
    }

    public ProfilResponse removeCompetence(String userId, String competence) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (utilisateur.getCompetences() != null) {
            utilisateur.getCompetences().remove(competence);
            utilisateur.setDateModification(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);
            log.info("Compétence '{}' supprimée pour l'utilisateur {}", competence, userId);
        }
        return toProfilResponse(utilisateur);
    }

    public void changerMotDePasse(String userId, ChangementMotDePasseRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getMotDePasseActuel(), utilisateur.getMotDePasse())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect");
        }

        if (!request.getNouveauMotDePasse().equals(request.getConfirmationMotDePasse())) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateur.setDateModification(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);
        log.info("Mot de passe modifié pour l'utilisateur {}", userId);
    }

    /**
     * Clé objet du CV ou d'une pièce du candidat lui-même (aucun contrôle
     * inter-utilisateur : le profil authentifié ne voit que ses fichiers).
     */
    public String getMonFichier(String userId, String type, Integer index) {
        ProfilCandidat profil = profilCandidatRepository.findByUtilisateurId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Fichier introuvable"));
        String key = extraireCle(profil, type, index);
        boolean present = false;
        try {
            present = key != null && !key.isBlank() && storage.exists(key);
        } catch (Exception e) {
            present = false;
        }
        if (!present) {
            throw new IllegalArgumentException("Fichier introuvable");
        }
        return key;
    }

    /**
     * Remplace le CV : stocke le nouveau, supprime l'ancien du backend, met à jour la base.
     */
    public ProfilResponse remplacerCv(String userId, MultipartFile file) {
        ProfilCandidat profil = profilCandidatRepository.findByUtilisateurId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Profil introuvable"));
        String newKey = inscriptionFileService.replaceCv(userId, profil.getCvFilename(), file);
        profil.setCvFilename(newKey);
        profilCandidatRepository.save(profil);
        log.info("CV remplacé pour l'utilisateur {}", userId);
        return toProfilResponse(profil.getUtilisateur());
    }

    /**
     * Ajoute des pièces complémentaires (limite totale de 5, comme à l'inscription).
     */
    public ProfilResponse ajouterPieces(String userId, List<MultipartFile> files) {
        ProfilCandidat profil = profilCandidatRepository.findByUtilisateurId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Profil introuvable"));
        List<MultipartFile> safe = (files == null) ? List.of() : files;
        if (safe.isEmpty()) {
            throw new IllegalArgumentException("Pièce complémentaire : aucun fichier fourni");
        }
        List<String> pieces = profil.getPiecesFilenames() != null
            ? new ArrayList<>(profil.getPiecesFilenames()) : new ArrayList<>();
        for (MultipartFile file : safe) {
            pieces.add(inscriptionFileService.addPiece(userId, pieces.size(), file));
        }
        profil.setPiecesFilenames(pieces);
        profilCandidatRepository.save(profil);
        log.info("Pièce(s) ajoutée(s) pour l'utilisateur {}", userId);
        return toProfilResponse(profil.getUtilisateur());
    }

    /**
     * Supprime une pièce : d'abord du backend (bucket OOS ou disque), puis de la base.
     * Si le backend ne contient déjà plus l'objet, la base est quand même nettoyée.
     */
    public ProfilResponse supprimerPiece(String userId, int index) {
        ProfilCandidat profil = profilCandidatRepository.findByUtilisateurId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Profil introuvable"));
        List<String> pieces = profil.getPiecesFilenames() != null
            ? new ArrayList<>(profil.getPiecesFilenames()) : new ArrayList<>();
        if (index < 0 || index >= pieces.size()) {
            throw new IllegalArgumentException("Pièce introuvable");
        }
        String key = pieces.remove(index);
        try {
            inscriptionFileService.deletePiece(key);
        } catch (Exception e) {
            log.warn("Pièce absente du backend pour l'utilisateur {}", userId);
        }
        profil.setPiecesFilenames(pieces);
        profilCandidatRepository.save(profil);
        log.info("Pièce supprimée pour l'utilisateur {}", userId);
        return toProfilResponse(profil.getUtilisateur());
    }

    private String extraireCle(ProfilCandidat profil, String type, Integer index) {
        if ("cv".equalsIgnoreCase(type)) {
            return profil.getCvFilename();
        }
        if ("piece".equalsIgnoreCase(type) && index != null
            && profil.getPiecesFilenames() != null
            && index >= 0 && index < profil.getPiecesFilenames().size()) {
            return profil.getPiecesFilenames().get(index);
        }
        return null;
    }

    private FichierDto toFichierDto(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        Long taille = null;
        try {
            if (storage.exists(key)) {
                taille = storage.size(key);
            }
        } catch (Exception e) {
            log.debug("Impossible de calculer la taille du fichier {}", key);
        }
        return new FichierDto(StoragePort.displayName(key), taille);
    }

    private ProfilResponse toProfilResponse(Utilisateur u) {
        ProfilCandidat profil = profilCandidatRepository.findByUtilisateurId(u.getId()).orElse(null);

        ProfilResponse response = new ProfilResponse(
            profil != null ? profil.getCivilite() : null,
            u.getNom(),
            u.getPrenom(),
            profil != null ? profil.getSituation() : null,
            u.getEmail(),
            profil != null ? profil.getTelephone() : null
        );
        response.setPhotoUrl(u.getPhotoUrl());
        response.setCompetences(u.getCompetences() != null ? new ArrayList<>(u.getCompetences()) : new ArrayList<>());
        response.setNbCandidatures(u.getNbCandidatures() != null ? u.getNbCandidatures() : 0);
        response.setOrcid(profil != null ? profil.getOrcid() : null);
        response.setEtablissement(profil != null ? profil.getEtablissement() : null);
        response.setLaboratoire(profil != null ? profil.getLaboratoire() : null);
        if (profil != null) {
            response.setCv(toFichierDto(profil.getCvFilename()));
            List<FichierDto> pieces = new ArrayList<>();
            if (profil.getPiecesFilenames() != null) {
                for (String key : profil.getPiecesFilenames()) {
                    pieces.add(toFichierDto(key));
                }
            }
            response.setPieces(pieces);
        }
        return response;
    }
}
