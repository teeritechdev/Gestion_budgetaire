package com.budget.backend.achat.entity;

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
@Table(name = "bons_commande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BonCommande {

    public static final String EMIS = "EMIS";
    public static final String EN_COURS = "EN_COURS";
    public static final String SOLDE = "SOLDE";
    public static final String ANNULE = "ANNULE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_bc", unique = true, nullable = false, length = 50)
    private String codeBc;

    @Builder.Default
    @Column(name = "type_bc", length = 30)
    private String typeBc = "CLASSIQUE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "da_id")
    private DemandeAchat demandeAchat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @Column(name = "montant_total_ht", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantTotalHt;

    @Column(name = "montant_total_ttc", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantTotalTtc;

    @Builder.Default
    @Column(name = "montant_facture_ttc", precision = 15, scale = 2)
    private BigDecimal montantFactureTtc = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String statut = EMIS;

    @Builder.Default
    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission = LocalDate.now();

    @Builder.Default
    @OneToMany(mappedBy = "bonCommande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneBonCommande> lignes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

