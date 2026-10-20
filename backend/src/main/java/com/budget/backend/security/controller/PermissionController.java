package com.budget.backend.security.controller;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.service.PermissionService;
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
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

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
    @PreAuthorize("hasPermission(null, 'permission.lire')")
    public ResponseEntity<List<Permission>> listerOuFiltrer(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String module) {
        if (code != null || libelle != null || module != null) {
            return ResponseEntity.ok(permissionService.filtrerPermissions(code, libelle, module));
        }
        return ResponseEntity.ok(permissionService.listerToutesLesPermissions());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'permission.lire')")
    public ResponseEntity<Permission> obtenirParId(@PathVariable UUID id) {
        return permissionService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'permission.creer')")
    public ResponseEntity<Permission> creer(@RequestBody Permission permission, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Permission nouvellePermission = permissionService.creerPermission(permission, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouvellePermission);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'permission.modifier')")
    public ResponseEntity<Permission> modifier(@PathVariable UUID id, @RequestBody Permission permission, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Permission permissionModifiee = permissionService.modifierPermission(id, permission, userId);
        return ResponseEntity.ok(permissionModifiee);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'permission.supprimer')")
    public ResponseEntity<?> supprimer(@PathVariable UUID id, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        permissionService.supprimerPermission(id, userId);
        return ResponseEntity.ok(Map.of("message", "Permission supprimée avec succès."));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'permission.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String module) {
        String csvData = permissionService.exporterEnCsv(code, libelle, module);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "permissions_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'permission.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String module) {
        byte[] pdfBytes = permissionService.exporterEnPdf(code, libelle, module);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "permissions_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/import/csv")
    @PreAuthorize("hasPermission(null, 'permission.importer')")
    public ResponseEntity<?> importerCsv(@RequestParam("file") MultipartFile file, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        int nb = permissionService.importerCsv(file, userId);
        return ResponseEntity.ok(Map.of("message", "Importation des permissions réussie.", "elementsImportes", nb));
    }
}