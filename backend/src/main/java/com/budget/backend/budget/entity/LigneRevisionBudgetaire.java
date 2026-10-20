package com.budget.backend.budget.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lignes_revision_budgetaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LigneRevisionBudgetaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_id", nullable = false)
    private DemandeRevisionBudgetaire demandeRevisionBudgetaire;

    @Column(name = "type_mouvement", nullable = false, length = 50)
    private String typeMouvement; // VIREMENT_INTERNE, EXTENSION

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_source_id")
    private BudgetAllocationStructure allocationSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_destination_id")
    private BudgetAllocationStructure allocationDestination;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}