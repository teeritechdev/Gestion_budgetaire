package com.budget.backend.abonnement.entity;

import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.security.entity.Utilisateur;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "abonnements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Abonnement {

    public static final String BROUILLON = "BROUILLON";
    public static final String ACTIF = "ACTIF";
    public static final String SUSPENDU = "SUSPENDU";
    public static final String RESILIE = "RESILIE";
    public static final String EXPIRE = "EXPIRE";

    public static final String MENSUEL = "MENSUEL";
    public static final String TRIMESTRIEL = "TRIMESTRIEL";
    public static final String SEMESTRIEL = "SEMESTRIEL";
    public static final String ANNUEL = "ANNUEL";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_abonnement", unique = true, nullable = false, length = 50)
    private String codeAbonnement; // Ex: ABN-2026-0012

    @Column(nullable = false, length = 255)
    private String libelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    // Ligne budgétaire imputée (équivalent de "activites_budgetaires" du MRD)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activite_id", nullable = false)
    private BudgetAllocationStructure activite;

    @Column(nullable = false, length = 20)
    private String periodicite;

    @Column(name = "jour_facturation", nullable = false)
    private Integer jourFacturation; // 31 = dernier jour du mois

    @Column(name = "montant_echeance_ht", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantEcheanceHt;

    @Column(name = "montant_echeance_ttc", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantEcheanceTtc;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Builder.Default
    @Column(name = "tacite_reconduction")
    private Boolean taciteReconduction = false;

    @Column(name = "preavis_jours")
    private Integer preavisJours;

    @Builder.Default
    @Column(length = 20)
    private String statut = BROUILLON;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id", nullable = false)
    private Utilisateur responsable;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Dernier jour pour notifier une résiliation (null si pas de préavis).
     * Le contrat court jusqu'à la fin de la journée date_fin : un préavis de N jours doit donc être donné
     * au plus tard le jour date_fin - N - 1. Pour l'exemple du PDF (fin 31/12/2026, préavis 60 jours),
     * cela donne le 31/10/2026, la date que le scénario cite ; c'est aussi la plus prudente, car une alerte
     * trop tôt ne coûte rien alors qu'une alerte trop tard fait reconduire le contrat.
     */
    public LocalDate dateLimitePreavis() {
        return preavisJours == null ? null : dateFin.minusDays(preavisJours + 1L);
    }
}
