package fr.dinum.beta.gouv.doctorat.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.MessageDto;
import fr.dinum.beta.gouv.doctorat.entity.DemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.entity.MessageEchange;
import fr.dinum.beta.gouv.doctorat.entity.ProfilDirecteurThese;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.StatutDemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.MessageEchangeRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.util.EmailUtils;

/**
 * Échanges de messages entre le candidat et le directeur de thèse
 * à propos d'une demande de mise en relation acceptée.
 * Une demande acceptée correspond à un fil de discussion unique.
 */
@Service
public class EchangeService {

    private static final Logger log = LoggerFactory.getLogger(EchangeService.class);

    private final DemandeMiseEnRelationRepository demandeRepository;
    private final MessageEchangeRepository messageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProfilDirecteurTheseRepository profilDtRepository;
    private final PropositionTheseRepository propositionTheseRepository;

    public EchangeService(DemandeMiseEnRelationRepository demandeRepository,
                          MessageEchangeRepository messageRepository,
                          UtilisateurRepository utilisateurRepository,
                          ProfilDirecteurTheseRepository profilDtRepository,
                          PropositionTheseRepository propositionTheseRepository) {
        this.demandeRepository = demandeRepository;
        this.messageRepository = messageRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.profilDtRepository = profilDtRepository;
        this.propositionTheseRepository = propositionTheseRepository;
    }

    /**
     * Liste les messages d'une demande et marque comme lus
     * ceux envoyés par l'autre partie.
     */
    public List<MessageDto> lister(String userId, Long demandeId) {
        DemandeMiseEnRelation demande = verifierAcces(userId, demandeId);
        List<MessageEchange> messages = messageRepository.findByDemandeIdOrderByCreatedAtAsc(demande.getId());

        List<MessageEchange> aMarquerLus = new ArrayList<>();
        for (MessageEchange m : messages) {
            if (!Boolean.TRUE.equals(m.getLu()) && !userId.equals(m.getAuteurId())) {
                m.setLu(Boolean.TRUE);
                aMarquerLus.add(m);
            }
        }
        if (!aMarquerLus.isEmpty()) {
            messageRepository.saveAll(aMarquerLus);
        }
        return toDto(userId, messages);
    }

    /**
     * Envoie un message sur une demande acceptée.
     */
    public MessageDto envoyer(String userId, Long demandeId, String contenu) {
        DemandeMiseEnRelation demande = verifierAcces(userId, demandeId);
        String texte = contenu != null ? contenu.trim() : "";
        if (texte.isEmpty()) {
            throw new IllegalArgumentException("Le message est obligatoire");
        }
        if (texte.length() > 2000) {
            throw new IllegalArgumentException("Le message ne doit pas dépasser 2000 caractères");
        }
        MessageEchange message = new MessageEchange(demande.getId(), userId, texte);
        messageRepository.save(message);
        log.info("Message envoyé sur la demande {} par l'utilisateur {}", demande.getId(), userId);
        return toDto(userId, List.of(message)).get(0);
    }

    /**
     * Vérifie que la demande est acceptée, non archivée et que
     * l'utilisateur est participant (candidat ou DT rattaché).
     */
    private DemandeMiseEnRelation verifierAcces(String userId, Long demandeId) {
        DemandeMiseEnRelation demande = demandeRepository.findById(demandeId)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        if (demande.getStatut() != StatutDemandeMiseEnRelation.ACCEPTEE
            || Boolean.TRUE.equals(demande.getArchivee())) {
            throw new IllegalArgumentException("Demande introuvable");
        }
        if (userId != null && userId.equals(demande.getCandidatId())) {
            return demande;
        }
        if (findSujetsRattaches(userId).containsKey(demande.getPropositionTheseId())) {
            return demande;
        }
        throw new IllegalArgumentException("Demande introuvable");
    }

    private List<MessageDto> toDto(String userId, List<MessageEchange> messages) {
        List<String> auteurIds = messages.stream()
            .map(MessageEchange::getAuteurId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        Map<String, Utilisateur> auteurs = new LinkedHashMap<>();
        if (!auteurIds.isEmpty()) {
            for (Utilisateur u : utilisateurRepository.findAllById(auteurIds)) {
                auteurs.put(u.getId(), u);
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<MessageDto> dtos = new ArrayList<>();
        for (MessageEchange m : messages) {
            MessageDto dto = new MessageDto();
            dto.setId(m.getId());
            dto.setDemandeId(m.getDemandeId());
            dto.setAuteurId(m.getAuteurId());
            dto.setEnvoyeParMoi(userId != null && userId.equals(m.getAuteurId()));
            Utilisateur auteur = auteurs.get(m.getAuteurId());
            if (auteur != null) {
                String nom = ((auteur.getPrenom() != null ? auteur.getPrenom().trim() + " " : "")
                    + (auteur.getNom() != null ? auteur.getNom().trim() : "")).trim();
                dto.setAuteurNom(nom.isEmpty() ? null : nom);
            }
            dto.setContenu(m.getContenu());
            dto.setDateEnvoi(m.getCreatedAt() != null ? m.getCreatedAt().format(formatter) : null);
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * Sujets rattachés au DT par comparaison de son e-mail et/ou ORCID
     * avec les champs direction/codirection des propositions.
     */
    private Map<Long, PropositionThese> findSujetsRattaches(String userId) {
        Map<Long, PropositionThese> sujets = new LinkedHashMap<>();
        Utilisateur utilisateur = utilisateurRepository.findById(userId).orElse(null);
        if (utilisateur == null) {
            return sujets;
        }
        ProfilDirecteurThese profil = profilDtRepository.findByUtilisateurId(userId).orElse(null);

        String email = EmailUtils.normalize(utilisateur.getEmail());
        String orcid = profil != null && profil.getOrcid() != null && !profil.getOrcid().trim().isEmpty()
            ? profil.getOrcid().trim() : null;

        if (email != null) {
            for (PropositionThese p : propositionTheseRepository.findByDirecteurEmail(email)) {
                if (p.getId() != null) {
                    sujets.putIfAbsent(p.getId(), p);
                }
            }
        }
        if (orcid != null) {
            for (PropositionThese p : propositionTheseRepository.findByDirecteurOrcid(orcid)) {
                if (p.getId() != null) {
                    sujets.putIfAbsent(p.getId(), p);
                }
            }
        }
        return sujets;
    }
}
