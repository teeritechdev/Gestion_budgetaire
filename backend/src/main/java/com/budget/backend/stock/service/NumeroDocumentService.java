package com.budget.backend.stock.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.time.Year;

/** Génère des numéros de document uniques via des séquences PostgreSQL (sûr en cas de requêtes simultanées). */
@Service
public class NumeroDocumentService {

    @PersistenceContext
    private EntityManager em;

    /** Ex: BS-2026-0042 */
    public String prochainNumeroBonSortie() {
        return prochain("BS", "seq_demande_magasin");
    }

    /** Ex: DA-2026-0045 */
    public String prochainCodeDemandeAchat() {
        return prochain("DA", "seq_demande_achat");
    }

    /** Ex: BC-2026-0089 */
    public String prochainCodeBonCommande() {
        return prochain("BC", "seq_bon_commande");
    }

    /** Ex: FOURN-2026-0012 */
    public String prochainCodeFournisseur() {
        return prochain("FOURN", "seq_fournisseur");
    }

    /** Ex: ABN-2026-0012 */
    public String prochainCodeAbonnement() {
        return prochain("ABN", "seq_abonnement");
    }

    /**
     * Numéro générique au format PREFIXE-AAAA-NNNN depuis une séquence PostgreSQL (à créer dans le script SQL du
     * module : CREATE SEQUENCE seq_xxx). Permet à un autre module d'obtenir sa numérotation sans modifier cette classe.
     * Exemple : prochainNumero("BL", "seq_bon_livraison") donne BL-2026-0001.
     */
    public String prochainNumero(String prefixe, String sequence) {
        if (prefixe == null || !prefixe.matches("[A-Z][A-Z0-9-]{0,19}") || sequence == null || !sequence.matches("seq_[a-z0-9_]{1,60}")) {
            throw new IllegalArgumentException("Préfixe ou nom de séquence invalide : " + prefixe + " / " + sequence);
        }
        return prochain(prefixe, sequence);
    }

    private String prochain(String prefixe, String sequence) {
        Number n = (Number) em.createNativeQuery("select nextval('" + sequence + "')").getSingleResult();
        return String.format("%s-%d-%04d", prefixe, Year.now().getValue(), n.longValue());
    }
}
