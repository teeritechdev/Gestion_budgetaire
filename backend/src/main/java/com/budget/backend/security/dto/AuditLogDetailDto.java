package com.budget.backend.security.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Vue détaillée d'une entrée du journal d'audit : auteur résolu, poste, requête et valeurs champ par champ. */
public record AuditLogDetailDto(
        UUID id,
        LocalDateTime createdAt,
        String typeAction,
        String nomTable,
        String enregistrementId,
        UUID utilisateurId,
        /** Nom affichable : « Prénom Nom (identifiant) », « Non authentifié (…) », « Système / anonyme »… */
        String auteur,
        String auteurUsername,
        String auteurMatricule,
        String auteurRole,
        String adresseIp,
        String userAgent,
        /** Résumé du user agent, ex. « Edge 156 · Windows 10/11 ». */
        String navigateur,
        /** Ex. « PUT /api/salaries/… — HTTP 200 », ou « — ». */
        String requete,
        List<Champ> champs,
        Map<String, Object> anciennesValeurs,
        Map<String, Object> nouvellesValeurs
) {
    public record Champ(String nom, String ancienneValeur, String nouvelleValeur, boolean modifie) {}
}
