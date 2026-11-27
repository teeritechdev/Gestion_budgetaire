package com.budget.backend.stock.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_article", unique = true, nullable = false, length = 50)
    private String codeArticle; // Ex: ART-PAP-001

    @Column(nullable = false, length = 255)
    private String designation;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "unite_mesure", nullable = false, length = 30)
    private String uniteMesure; // RAME, CARTON, UNITE, PAQUET, BOITE

    @Builder.Default
    @Column(name = "quantite_stock")
    private Integer quantiteStock = 0;

    @Builder.Default
    @Column(name = "seuil_reapprovisionnement")
    private Integer seuilReapprovisionnement = 0;

    @Builder.Default
    @Column(name = "prix_unitaire_moyen_pondere", precision = 15, scale = 2)
    private BigDecimal prixUnitaireMoyenPondere = BigDecimal.ZERO;

    @Builder.Default
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
