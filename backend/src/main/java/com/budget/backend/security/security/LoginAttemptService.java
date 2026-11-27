package com.budget.backend.security.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite les tentatives de connexion échouées par identifiant pour freiner le brute force.
 * Après {@link #MAX_ECHECS} échecs dans la fenêtre, le compte est bloqué pendant {@link #DUREE_BLOCAGE}.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ECHECS = 5;
    static final Duration FENETRE = Duration.ofMinutes(15);
    static final Duration DUREE_BLOCAGE = Duration.ofMinutes(15);

    private record Tentatives(int echecs, Instant premierEchec, Instant bloqueJusqua) {}

    private final Map<String, Tentatives> tentatives = new ConcurrentHashMap<>();

    public boolean estBloque(String username) {
        Tentatives etat = tentatives.get(cle(username));
        return etat != null && etat.bloqueJusqua() != null && etat.bloqueJusqua().isAfter(Instant.now());
    }

    public void echec(String username) {
        Instant maintenant = Instant.now();
        tentatives.compute(cle(username), (cle, etat) -> {
            if (etat == null || etat.premierEchec().plus(FENETRE).isBefore(maintenant)) {
                return new Tentatives(1, maintenant, null);
            }
            int echecs = etat.echecs() + 1;
            Instant blocage = echecs >= MAX_ECHECS ? maintenant.plus(DUREE_BLOCAGE) : etat.bloqueJusqua();
            return new Tentatives(echecs, etat.premierEchec(), blocage);
        });
    }

    public void succes(String username) {
        tentatives.remove(cle(username));
    }

    private String cle(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
