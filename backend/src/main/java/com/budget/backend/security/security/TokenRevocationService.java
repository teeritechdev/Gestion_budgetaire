package com.budget.backend.security.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Liste des tokens JWT révoqués (déconnexion), conservés jusqu'à leur expiration naturelle.
 * Stockage en mémoire : suffisant pour une instance unique du backend.
 */
@Service
public class TokenRevocationService {

    private final Map<String, Instant> tokensRevoques = new ConcurrentHashMap<>();

    public void revoquer(String tokenId, Instant expiration) {
        if (tokenId == null || expiration == null) {
            return;
        }
        purgerExpires();
        tokensRevoques.put(tokenId, expiration);
    }

    public boolean estRevoque(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        Instant expiration = tokensRevoques.get(tokenId);
        if (expiration == null) {
            return false;
        }
        if (expiration.isBefore(Instant.now())) {
            tokensRevoques.remove(tokenId);
            return false;
        }
        return true;
    }

    private void purgerExpires() {
        Instant maintenant = Instant.now();
        tokensRevoques.entrySet().removeIf(entry -> entry.getValue().isBefore(maintenant));
    }
}
