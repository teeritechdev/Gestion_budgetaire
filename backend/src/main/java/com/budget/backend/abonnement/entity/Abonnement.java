package com.budget.backend.abonnement.entity;

import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.budget.entity.ActiviteCatalogue;
import com.budget.backend.security.entity.Utilisateur;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_abonnement", unique = true, nullable = false, length = 50)
    private String codeAbonnement;

    @Column(nullable = false, length = 255)
    private String libelle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activite_id", nullable = false)
    private ActiviteCatalogue activite;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String periodicite = "MENSUEL"; // MENSUEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL

    @Builder.Default
    @Column(name = "jour_facturation", nullable = false)
    private Integer jourFacturation = 31;

    @Column(name = "montant_echeance_ht", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantEcheanceHt;

    @Column(name = "montant_echeance_ttc", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantEcheanceTtc;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Builder.Default
    @Column(name = "tacite_reconduction")
    private Boolean taciteReconduction = false;

    @Builder.Default
    @Column(name = "preavis_jours")
    private Integer preavisJours = 30;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String statut = ACTIF;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Utilisateur responsable;

    @Builder.Default
    @OneToMany(mappedBy = "abonnement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcheanceAbonnement> echeances = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

