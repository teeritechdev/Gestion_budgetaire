package com.budget.backend.security.service;

import com.budget.backend.security.dto.AffectationResumeDto;
import com.budget.backend.security.dto.ProfilDto;
import com.budget.backend.security.entity.PhotoProfil;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.PhotoProfilRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** Profil de l'utilisateur connecté et photo de profil. */
@Service
public class ProfilService {

    /** Taille maximale d'une photo (le front la redimensionne avant l'envoi, bien en dessous). */
    static final int TAILLE_MAX_PHOTO = 2 * 1024 * 1024;

    private final UtilisateurRepository utilisateurRepository;
    private final PhotoProfilRepository photoRepository;
    private final AuditService auditService;

    private final AffectationService affectationService;

    public ProfilService(UtilisateurRepository utilisateurRepository, PhotoProfilRepository photoRepository,
                         AuditService auditService, AffectationService affectationService) {
        this.utilisateurRepository = utilisateurRepository;
        this.photoRepository = photoRepository;
        this.auditService = auditService;
        this.affectationService = affectationService;
    }

    @Transactional(readOnly = true)
    public ProfilDto profil(String username) {
        Utilisateur utilisateur = trouver(username);
        Salarie salarie = utilisateur.getSalarie();
        AffectationResumeDto affectation = salarie == null ? null : affectationService.resumeActuelle(salarie);
        Optional<Instant> photo = photoRepository.findById(utilisateur.getId()).map(PhotoProfil::getModifieeLe);
        return new ProfilDto(
                utilisateur.getId(),
                utilisateur.getUsername(),
                salarie == null ? null : salarie.getNom(),
                salarie == null ? null : salarie.getPrenom(),
                salarie == null ? null : salarie.getSexe(),
                salarie == null ? null : salarie.getEmail(),
                salarie == null ? null : salarie.getTelephone(),
                salarie == null ? null : salarie.getMatricule(),
                salarie == null ? null : salarie.getPoste(),
                salarie == null ? null : salarie.getDateEmbauche(),
                affectation == null ? null : affectation.serviceLibelle(),
                affectation == null ? null : affectation.directionLibelle(),
                affectation == null ? null : affectation.structureCode(),
                utilisateur.getRole() == null ? null : utilisateur.getRole().getCode(),
                utilisateur.getRole() == null ? null : utilisateur.getRole().getLibelle(),
                utilisateur.getCreatedAt(),
                utilisateur.getMotDePasseModifieLe(),
                Boolean.TRUE.equals(utilisateur.getDoitChangerMotDePasse()),
                photo.isPresent(),
                photo.orElse(null));
    }

    @Transactional(readOnly = true)
    public Optional<PhotoProfil> photo(String username) {
        return photoRepository.findById(trouver(username).getId());
    }

    @Transactional(readOnly = true)
    public boolean avecPhoto(Utilisateur utilisateur) {
        return photoRepository.existsById(utilisateur.getId());
    }

    @Transactional
    public ProfilDto enregistrerPhoto(String username, MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune image reçue.");
        }
        if (fichier.getSize() > TAILLE_MAX_PHOTO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "L'image ne doit pas dépasser 2 Mo.");
        }
        byte[] contenu;
        try {
            contenu = fichier.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image illisible.");
        }
        // Type déterminé par le contenu réel du fichier, pas par son nom ni par l'en-tête envoyé.
        String typeMime = typeImage(contenu);
        if (typeMime == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Format non pris en charge : utilisez une image JPEG, PNG ou WebP.");
        }
        Utilisateur utilisateur = trouver(username);
        PhotoProfil photo = photoRepository.findById(utilisateur.getId())
                .orElseGet(() -> PhotoProfil.builder().utilisateurId(utilisateur.getId()).build());
        photo.setContenu(contenu);
        photo.setTypeMime(typeMime);
        photo.setTaille(contenu.length);
        photo.setModifieeLe(Instant.now());
        photoRepository.save(photo);
        auditService.logAction(utilisateur.getId(), "PHOTO_PROFIL_MODIFIEE", "utilisateurs",
                utilisateur.getId().toString(), null, Map.of("format", typeMime, "tailleOctets", contenu.length));
        return profil(username);
    }

    @Transactional
    public ProfilDto supprimerPhoto(String username) {
        Utilisateur utilisateur = trouver(username);
        if (photoRepository.existsById(utilisateur.getId())) {
            photoRepository.deleteById(utilisateur.getId());
            auditService.logAction(utilisateur.getId(), "PHOTO_PROFIL_SUPPRIMEE", "utilisateurs",
                    utilisateur.getId().toString(), null, null);
        }
        return profil(username);
    }

    private Utilisateur trouver(String username) {
        return utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide."));
    }

    /** JPEG, PNG ou WebP d'après la signature du fichier ; null sinon. */
    static String typeImage(byte[] c) {
        if (c.length >= 3 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (c.length >= 8 && (c[0] & 0xFF) == 0x89 && c[1] == 'P' && c[2] == 'N' && c[3] == 'G'
                && c[4] == 0x0D && c[5] == 0x0A && c[6] == 0x1A && c[7] == 0x0A) return "image/png";
        if (c.length >= 12 && c[0] == 'R' && c[1] == 'I' && c[2] == 'F' && c[3] == 'F'
                && c[8] == 'W' && c[9] == 'E' && c[10] == 'B' && c[11] == 'P') return "image/webp";
        return null;
    }
}
