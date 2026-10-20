package com.budget.backend.security.controller;

import com.budget.backend.security.entity.Role;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.service.RoleService;
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
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RoleController {

    @Autowired
    private RoleService roleService;

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
    @PreAuthorize("hasPermission(null, 'role.lire')")
    public ResponseEntity<List<Role>> listerOuFiltrer(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String libelle) {
        if (code != null || libelle != null) {
            return ResponseEntity.ok(roleService.filtrerRoles(code, libelle));
        }
        return ResponseEntity.ok(roleService.listerTousLesRoles());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'role.lire')")
    public ResponseEntity<Role> obtenirParId(@PathVariable UUID id) {
        return roleService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'role.creer')")
    public ResponseEntity<Role> creerRole(@RequestBody RoleRequestDto request, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Role nouveauRole = roleService.creerRole(request.getRole(), request.getPermissionIds(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouveauRole);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'role.modifier')")
    public ResponseEntity<Role> modifierRole(@PathVariable UUID id, @RequestBody RoleRequestDto request, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Role roleModifie = roleService.modifierRole(id, request.getRole(), request.getPermissionIds(), userId);
        return ResponseEntity.ok(roleModifie);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'role.supprimer')")
    public ResponseEntity<?> supprimerRole(@PathVariable UUID id, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        roleService.supprimerRole(id, userId);
        return ResponseEntity.ok(Map.of("message", "Rôle supprimé avec succès."));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'role.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String libelle) {
        String csvData = roleService.exporterEnCsv(code, libelle);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "roles_export.csv");

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'role.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String libelle) {
        byte[] pdfBytes = roleService.exporterEnPdf(code, libelle);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "roles_export.pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/import/csv")
    @PreAuthorize("hasPermission(null, 'role.importer')")
    public ResponseEntity<?> importerCsv(@RequestParam("file") MultipartFile file, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        int nbImportes = roleService.importerCsv(file, userId);
        return ResponseEntity.ok(Map.of("message", "Importation réussie.", "elementsImportes", nbImportes));
    }

    // --- DTO pour encapsuler l'entité Role et la liste des UUID de permissions ---
    @lombok.Data
    public static class RoleRequestDto {
        private Role role;
        private List<UUID> permissionIds;
    }
}