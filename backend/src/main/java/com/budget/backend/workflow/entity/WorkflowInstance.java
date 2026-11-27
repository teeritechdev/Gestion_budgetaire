package com.budget.backend.workflow.entity;

import com.budget.backend.security.entity.Utilisateur;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WorkflowInstance {

    public static final String EN_ATTENTE_VALIDATION = "EN_ATTENTE_VALIDATION";
    public static final String APPROUVE = "APPROUVE";
    public static final String REJETE = "REJETE";
    public static final String ANNULE = "ANNULE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowDefinition workflow;

    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    @Column(name = "type_entite", nullable = false, length = 50)
    private String typeEntite;

    @Column(name = "reference_dossier", nullable = false, length = 100)
    private String referenceDossier;

    @Column(length = 255)
    private String objet;

    @Builder.Default
    @Column(name = "montant_total", precision = 15, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id")
    private Utilisateur demandeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_courante_id")
    private WorkflowEtape etapeCourante;

    @Builder.Default
    @Column(name = "statut_actuel", length = 50)
    private String statutActuel = EN_ATTENTE_VALIDATION;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

