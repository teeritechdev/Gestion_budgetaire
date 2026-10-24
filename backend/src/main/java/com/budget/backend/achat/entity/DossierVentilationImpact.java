package com.budget.backend.achat.entity;

import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.security.entity.Entite;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dossier_ventilation_impacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DossierVentilationImpact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "da_id", nullable = false)
    private DemandeAchat demandeAchat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entite_impactee", referencedColumnName = "code", nullable = false)
    private Entite entiteImpactee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activite_impactee_id")
    private BudgetAllocationStructure activiteImpactee;

    @Column(name = "montant_impute", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantImpute;

    @Column(name = "pourcentage_applique", precision = 5, scale = 2, nullable = false)
    private BigDecimal pourcentageApplique;

    @Builder.Default
    @Column(name = "statut_blocage")
    private Boolean statutBlocage = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
