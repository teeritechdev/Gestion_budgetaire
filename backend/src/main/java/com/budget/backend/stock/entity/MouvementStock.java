package com.budget.backend.stock.entity;

import com.budget.backend.security.entity.Utilisateur;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mouvements_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MouvementStock {

    public static final String ENTREE = "ENTREE";
    public static final String SORTIE = "SORTIE";
    public static final String AJUSTEMENT = "AJUSTEMENT";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "type_mouvement", nullable = false, length = 30)
    private String typeMouvement;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "stock_anterieur", nullable = false)
    private Integer stockAnterieur;

    @Column(name = "nouveau_stock", nullable = false)
    private Integer nouveauStock;

    @Column(name = "reference_document", nullable = false, length = 100)
    private String referenceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Utilisateur auteur;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
