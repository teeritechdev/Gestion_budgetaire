package com.budget.backend.workflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowDefinition workflow;

    @Column(name = "num_etape", nullable = false)
    private Integer numEtape;

    @Column(name = "code_etape", nullable = false, length = 50)
    private String codeEtape; // Ex: VAL_DFI, APP_DG

    @Column(nullable = false, length = 255)
    private String libelle;

    // Permission que le rôle de l'utilisateur doit posséder (ex. ENGAGEMENT_VALIDER_DFI ou demande_achat.viser)
    @Column(name = "permission_requise", nullable = false, length = 100)
    private String permissionRequise;

    @Column(name = "montant_minimum", precision = 15, scale = 2)
    private BigDecimal montantMinimum;

    @Column(name = "montant_maximum", precision = 15, scale = 2)
    private BigDecimal montantMaximum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_suivante_succes_id")
    private WorkflowEtape etapeSuivanteSucces;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_suivante_rejet_id")
    private WorkflowEtape etapeSuivanteRejet;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Plage de montants (bornes incluses ; une borne absente est ouverte). Exemple du PDF : étape DG de 50 000,01
     * à l'infini, étape DFI de 0 à l'infini.
     */
    public boolean estApplicable(BigDecimal montant) {
        BigDecimal m = montant == null ? BigDecimal.ZERO : montant;
        return (montantMinimum == null || m.compareTo(montantMinimum) >= 0)
                && (montantMaximum == null || m.compareTo(montantMaximum) <= 0);
    }
}
