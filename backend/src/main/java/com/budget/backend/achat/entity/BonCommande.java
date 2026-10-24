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

    public static final String CLASSIQUE = "CLASSIQUE";
    public static final String CADRE_ANNUEL = "CADRE_ANNUEL";

    public static final String EMIS = "EMIS";
    public static final String ENVOYE_FOURNISSEUR = "ENVOYE_FOURNISSEUR";
    public static final String CADRE_EN_COURS = "CADRE_EN_COURS";
    public static final String PARTIELLEMENT_FACTURE = "PARTIELLEMENT_FACTURE";
    public static final String SOLDE = "SOLDE";
    public static final String ANNULE = "ANNULE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_bc", unique = true, nullable = false, length = 50)
    private String codeBc; // Ex: BC-2026-0089

    @Builder.Default
    @Column(name = "type_bc", length = 30)
    private String typeBc = CLASSIQUE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "da_id", nullable = false)
    private DemandeAchat demandeAchat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @Column(name = "montant_total_ht", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantTotalHt;

    @Column(name = "montant_total_ttc", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantTotalTtc;

    @Builder.Default
    @Column(name = "montant_facture_ttc", precision = 15, scale = 2)
    private BigDecimal montantFactureTtc = BigDecimal.ZERO;

    // Colonne générée par la BDD : montant_total_ttc - montant_facture_ttc
    @Column(name = "solde_restant_ttc", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal soldeRestantTtc;

    @Builder.Default
    @Column(length = 30)
    private String statut = EMIS;

    @Column(name = "date_emission")
    private LocalDate dateEmission;

    @Builder.Default
    @OneToMany(mappedBy = "bonCommande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneBonCommande> lignes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BigDecimal calculerSolde() {
        return montantTotalTtc.subtract(montantFactureTtc == null ? BigDecimal.ZERO : montantFactureTtc);
    }
}
