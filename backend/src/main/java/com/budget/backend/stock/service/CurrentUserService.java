package com.budget.backend.stock.service;

import com.budget.backend.stock.StockException;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/** Retrouve l'utilisateur connecté à partir du jeton JWT (lecture seule du module Sécurité). */
@Service
public class CurrentUserService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public Utilisateur utilisateurConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails details)) {
            throw new StockException("Aucun utilisateur authentifié.");
        }
        return utilisateurRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new StockException("Utilisateur connecté introuvable : " + details.getUsername()));
    }
}
