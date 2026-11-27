package com.budget.backend.stock.service;

import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Raccourci vers AuditService (Module 1, inchangé) pour les Modules 3 et 4 :
 * types d'action du PDF INSERTION, MODIFICATION, VALIDATION ; valeurs null-safe.
 */
@Component
public class AuditHelper {

    public static final String INSERTION = "INSERTION";
    public static final String MODIFICATION = "MODIFICATION";
    public static final String VALIDATION = "VALIDATION";

    @Autowired
    private AuditService auditService;

    public void tracer(Utilisateur auteur, String typeAction, String nomTable, UUID enregistrementId,
                       Map<String, Object> anciennes, Map<String, Object> nouvelles) {
        auditService.logAction(auteur != null ? auteur.getId() : null, typeAction, nomTable,
                enregistrementId != null ? enregistrementId.toString() : null, anciennes, nouvelles);
    }

    /** Construit une map clé/valeur en ignorant les valeurs null (Map.of les refuserait). */
    public static Map<String, Object> valeurs(Object... clesEtValeurs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < clesEtValeurs.length; i += 2) {
            if (clesEtValeurs[i + 1] != null) {
                map.put((String) clesEtValeurs[i], clesEtValeurs[i + 1].toString());
            }
        }
        return map;
    }
}
