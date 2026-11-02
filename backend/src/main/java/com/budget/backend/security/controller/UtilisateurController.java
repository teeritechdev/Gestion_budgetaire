package com.budget.backend.security.controller;

import com.budget.backend.security.dto.UtilisateurMotDePasseRequest;
import com.budget.backend.security.dto.UtilisateurRequestDto;
import com.budget.backend.security.dto.UtilisateurResponseDto;
import jakarta.validation.Valid;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.service.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    private String getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) return null;
        User user = (User) authentication.getPrincipal();
        return utilisateurRepository.findByUsername(user.getUsername())
                .map(u -> u.getId().toString())
                .orElse(null);
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'utilisateur.lire')")
    public ResponseEntity<List<UtilisateurResponseDto>> listerOuFiltrer(
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche) {
        if (serviceId != null || roleId != null || actif != null || recherche != null) {
            return ResponseEntity.ok(utilisateurService.filtrerUtilisateurs(serviceId, roleId, actif, recherche));
        }
        return ResponseEntity.ok(utilisateurService.listerTousLesUtilisateurs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'utilisateur.lire')")
    public ResponseEntity<UtilisateurResponseDto> obtenirParId(@PathVariable UUID id) {
        return utilisateurService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'utilisateur.creer')")
    public ResponseEntity<UtilisateurResponseDto> creer(
            @Valid @RequestBody UtilisateurMotDePasseRequest request,
            Authentication authentication) {
        if (request == null || request.utilisateur() == null || request.motDePasse() == null || request.motDePasse().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe est obligatoire.");
        }
        if (request.motDePasse().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins 8 caractères.");
        }
        validerProfilUtilisateur(request.utilisateur());
        String userId = getUserIdFromAuth(authentication);
        UtilisateurResponseDto nouveau = utilisateurService.creerUtilisateur(
                request.utilisateur(), request.motDePasse(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouveau);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'utilisateur.modifier')")
    public ResponseEntity<UtilisateurResponseDto> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody UtilisateurMotDePasseRequest request,
            Authentication authentication) {
        if (request == null || request.utilisateur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations utilisateur sont obligatoires.");
        }
        String nouveauMotDePasse = request.motDePasse();
        if (nouveauMotDePasse != null && !nouveauMotDePasse.isBlank() && nouveauMotDePasse.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins 8 caractères.");
        }
        validerProfilUtilisateur(request.utilisateur());
        String userId = getUserIdFromAuth(authentication);
        UtilisateurResponseDto modifie = utilisateurService.modifierUtilisateur(
                id, request.utilisateur(), nouveauMotDePasse, userId);
        return ResponseEntity.ok(modifie);
    }

    private void validerProfilUtilisateur(UtilisateurRequestDto utilisateur) {
        if (utilisateur.salarieId() == null && (utilisateur.sexe() == null
                || !java.util.Set.of("FEMME", "HOMME", "AUTRE", "NON_RENSEIGNE").contains(utilisateur.sexe()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez une valeur valide pour le sexe.");
        }
        if (utilisateur.telephone() != null && utilisateur.telephone().length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le numéro de téléphone ne peut pas dépasser 30 caractères.");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'utilisateur.supprimer')")
    public ResponseEntity<?> supprimer(@PathVariable UUID id, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        utilisateurService.supprimerUtilisateur(id, userId);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès."));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'utilisateur.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche) {
        String csvData = utilisateurService.exporterEnCsv(serviceId, roleId, actif, recherche);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "utilisateurs_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'utilisateur.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche) {
        byte[] pdfBytes = utilisateurService.exporterEnPdf(serviceId, roleId, actif, recherche);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "utilisateurs_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/import/csv")
    @PreAuthorize("hasPermission(null, 'utilisateur.importer')")
    public ResponseEntity<?> importerCsv(@RequestParam("file") MultipartFile file, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        int nb = utilisateurService.importerCsv(file, userId);
        return ResponseEntity.ok(Map.of("message", "Importation des utilisateurs réussie.", "elementsImportes", nb));
    }
}
