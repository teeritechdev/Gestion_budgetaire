package com.budget.backend.budget.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "journal_mouvements_budgetaires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class JournalMouvementsBudgetaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_structure_id", nullable = false)
    private BudgetAllocationStructure allocationStructure;

    @Column(name = "annee_exercice", nullable = false)
    private Integer anneeExercice;

    @Column(name = "type_mouvement", nullable = false, length = 50)
    private String typeMouvement;

    @Column(name = "montant_anterieur", precision = 15, scale = 2)
    private BigDecimal montantAnterieur;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal variation;

    @Column(name = "nouveau_montant", precision = 15, scale = 2, nullable = false)
    private BigDecimal nouveauMontant;

    @Column(name = "reference_document", length = 100)
    private String referenceDocument;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}