package com.budget.backend.budget.entity;

import com.budget.backend.security.entity.Structure;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "budget_allocation_structure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BudgetAllocationStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_activite_id", nullable = false)
    private BudgetAllocationActivite budgetAllocationActivite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    @Column(name = "montant_vote", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantVote;

    @Builder.Default
    @Column(name = "montant_modifie", precision = 15, scale = 2)
    private BigDecimal montantModifie = BigDecimal.ZERO;

    // Colonne générée par la BDD : montant_vote + montant_modifie
    @Column(name = "montant_alloue", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal montantAlloue;

    @Builder.Default
    @Column(name = "montant_engage", precision = 15, scale = 2)
    private BigDecimal montantEngage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "montant_liquide", precision = 15, scale = 2)
    private BigDecimal montantLiquide = BigDecimal.ZERO;

    // Colonne générée par la BDD : (montant_vote + montant_modifie) - (montant_engage + montant_liquide)
    @Column(name = "montant_disponible", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal montantDisponible;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}