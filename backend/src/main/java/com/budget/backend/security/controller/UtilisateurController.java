package com.budget.backend.security.controller;

import com.budget.backend.security.entity.Utilisateur;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UtilisateurController {

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    private String getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) return null;
        User user = (User) authentication.getPrincipal();
        return utilisateurRepository.findByEmail(user.getUsername())
                .map(u -> u.getId().toString())
                .orElse(null);
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'utilisateur.lire')")
    public ResponseEntity<List<Utilisateur>> listerOuFiltrer(
            @RequestParam(required = false) UUID structureId,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche) {
        if (structureId != null || roleId != null || actif != null || recherche != null) {
            return ResponseEntity.ok(utilisateurService.filtrerUtilisateurs(structureId, roleId, actif, recherche));
        }
        return ResponseEntity.ok(utilisateurService.listerTousLesUtilisateurs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'utilisateur.lire')")
    public ResponseEntity<Utilisateur> obtenirParId(@PathVariable UUID id) {
        return utilisateurService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'utilisateur.creer')")
    public ResponseEntity<Utilisateur> creer(
            @RequestBody Utilisateur utilisateur,
            @RequestParam String motDePasse,
            Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Utilisateur nouveau = utilisateurService.creerUtilisateur(utilisateur, motDePasse, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouveau);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'utilisateur.modifier')")
    public ResponseEntity<Utilisateur> modifier(
            @PathVariable UUID id,
            @RequestBody Utilisateur utilisateur,
            @RequestParam(required = false) String nouveauMotDePasse,
            Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Utilisateur modifie = utilisateurService.modifierUtilisateur(id, utilisateur, nouveauMotDePasse, userId);
        return ResponseEntity.ok(modifie);
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
            @RequestParam(required = false) UUID structureId,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche) {
        String csvData = utilisateurService.exporterEnCsv(structureId, roleId, actif, recherche);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "utilisateurs_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'utilisateur.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) UUID structureId,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche) {
        byte[] pdfBytes = utilisateurService.exporterEnPdf(structureId, roleId, actif, recherche);
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