package com.budget.backend.achat.entity;

import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.stock.entity.Article;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lignes_demande_achat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LigneDemandeAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "da_id", nullable = false)
    private DemandeAchat demandeAchat;

    // Ligne budgétaire à engager (équivalent de "activites_budgetaires" du MRD)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activite_id", nullable = false)
    private BudgetAllocationStructure activite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "unite_mesure", length = 30)
    private String uniteMesure;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite;

    @Column(name = "prix_unitaire_estime", precision = 15, scale = 2, nullable = false)
    private BigDecimal prixUnitaireEstime;

    // Colonne générée par la BDD : quantite * prix_unitaire_estime
    @Column(name = "montant_total_ht", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal montantTotalHt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Montant HT calculé en Java (la colonne générée n'est relue qu'après rechargement). */
    public BigDecimal calculerMontantHt() {
        BigDecimal montant = prixUnitaireEstime.multiply(quantite);
        return montant.scale() > 2 ? montant.setScale(2, java.math.RoundingMode.HALF_UP) : montant;
    }
}
