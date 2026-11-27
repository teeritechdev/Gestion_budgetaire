package com.budget.backend.achat.entity;

import com.budget.backend.stock.entity.Article;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lignes_bon_commande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LigneBonCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bc_id", nullable = false)
    private BonCommande bonCommande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_da_id")
    private LigneDemandeAchat ligneDa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantite_commandee")
    private Integer quantiteCommandee;

    @Column(name = "prix_unitaire_ht", precision = 15, scale = 2)
    private BigDecimal prixUnitaireHt;

    @Column(name = "montant_total_ht", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantTotalHt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

