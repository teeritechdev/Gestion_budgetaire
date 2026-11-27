package com.budget.backend.achat.entity;

import com.budget.backend.budget.entity.ActiviteCatalogue;
import com.budget.backend.stock.entity.Article;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "da_id", nullable = false)
    private DemandeAchat demandeAchat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activite_id", nullable = false)
    private ActiviteCatalogue activite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "unite_mesure", length = 30)
    private String uniteMesure;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "prix_unitaire_estime", nullable = false, precision = 15, scale = 2)
    private BigDecimal prixUnitaireEstime;

    @Column(name = "montant_total_ht", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantTotalHt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void calculerMontantTotal() {
        if (quantite != null && prixUnitaireEstime != null) {
            this.montantTotalHt = prixUnitaireEstime.multiply(BigDecimal.valueOf(quantite));
        } else {
            this.montantTotalHt = BigDecimal.ZERO;
        }
    }
}

