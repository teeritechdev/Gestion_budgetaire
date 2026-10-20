package com.budget.backend.security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TestController {

    @GetMapping("/all")
    public ResponseEntity<String> allAccess() {
        return ResponseEntity.ok("Contenu public accessible sans authentification.");
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> userAccess() {
        return ResponseEntity.ok("Accès autorisé pour tout utilisateur connecté.");
    }

    @GetMapping("/agent")
    @PreAuthorize("hasPermission(null, 'entite.lire')")
    public ResponseEntity<String> agentAccess() {
        return ResponseEntity.ok("Accès validé via la permission dynamique 'entite.lire' en base de données.");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasPermission(null, 'role.creer')")
    public ResponseEntity<String> adminAccess() {
        return ResponseEntity.ok("Accès validé via la permission dynamique 'role.creer' en base de données.");
    }
}