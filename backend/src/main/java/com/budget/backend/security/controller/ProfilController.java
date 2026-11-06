package com.budget.backend.security.controller;

import com.budget.backend.security.dto.ChangementMotDePasseRequest;
import com.budget.backend.security.dto.JwtResponse;
import com.budget.backend.security.dto.ProfilDto;
import com.budget.backend.security.security.ReponseConnexion;
import com.budget.backend.security.service.MotDePasseService;
import com.budget.backend.security.service.ProfilService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Profil de l'utilisateur connecté : accessible à tout compte authentifié, sans permission particulière. */
@RestController
@RequestMapping("/api/profil")
public class ProfilController {

    private final ProfilService profilService;
    private final MotDePasseService motDePasseService;
    private final ReponseConnexion reponseConnexion;

    public ProfilController(ProfilService profilService, MotDePasseService motDePasseService,
                            ReponseConnexion reponseConnexion) {
        this.profilService = profilService;
        this.motDePasseService = motDePasseService;
        this.reponseConnexion = reponseConnexion;
    }

    @GetMapping
    public ProfilDto profil(Authentication authentication) {
        return profilService.profil(authentication.getName());
    }

    /** Change le mot de passe ; les autres sessions sont fermées et une nouvelle session est renvoyée. */
    @PutMapping("/mot-de-passe")
    public JwtResponse changerMotDePasse(@Valid @RequestBody ChangementMotDePasseRequest request,
                                         Authentication authentication) {
        motDePasseService.changer(authentication.getName(), request.motDePasseActuel(), request.nouveauMotDePasse());
        return reponseConnexion.pour(authentication.getName());
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> photo(Authentication authentication) {
        return profilService.photo(authentication.getName())
                .map(photo -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(photo.getTypeMime()))
                        .cacheControl(CacheControl.noCache().cachePrivate())
                        .body(photo.getContenu()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfilDto enregistrerPhoto(@RequestParam("fichier") MultipartFile fichier, Authentication authentication) {
        return profilService.enregistrerPhoto(authentication.getName(), fichier);
    }

    @DeleteMapping("/photo")
    public ProfilDto supprimerPhoto(Authentication authentication) {
        return profilService.supprimerPhoto(authentication.getName());
    }
}
