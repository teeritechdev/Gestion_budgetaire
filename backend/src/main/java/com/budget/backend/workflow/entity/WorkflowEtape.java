package com.budget.backend.workflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_etapes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WorkflowEtape {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowDefinition workflow;

    @Column(name = "num_etape", nullable = false)
    private Integer numEtape;

    @Column(name = "code_etape", nullable = false, length = 50)
    private String codeEtape;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Column(name = "permission_requise", length = 100)
    private String permissionRequise;

    @Column(name = "role_requis", length = 50)
    private String roleRequis;

    @Builder.Default
    @Column(name = "montant_minimum", precision = 15, scale = 2)
    private BigDecimal montantMinimum = BigDecimal.ZERO;

    @Column(name = "montant_maximum", precision = 15, scale = 2)
    private BigDecimal montantMaximum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_suivante_succes_id")
    private WorkflowEtape etapeSuivanteSucces;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_suivante_rejet_id")
    private WorkflowEtape etapeSuivanteRejet;

    @Builder.Default
    @Column(name = "est_etape_finale")
    private Boolean estEtapeFinale = false;

    @Builder.Default
    @Column(name = "est_etape_rejet")
    private Boolean estEtapeRejet = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

