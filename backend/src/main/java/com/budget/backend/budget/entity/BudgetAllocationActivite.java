package com.budget.backend.budget.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "budget_allocation_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BudgetAllocationActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_op_id", nullable = false)
    private BudgetAllocationOp budgetAllocationOp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activite_catalogue_id", nullable = false)
    private ActiviteCatalogue activiteCatalogue;

    @Column(name = "montant_alloue", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantAlloue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}