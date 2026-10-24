package com.budget.backend.workflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowDefinition workflow;

    // Clé fonctionnelle vers la table métier (ex. demandes_achat.id) : volontairement sans clé étrangère
    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_courante_id")
    private WorkflowEtape etapeCourante;

    @Column(name = "statut_actuel", nullable = false, length = 30)
    private String statutActuel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
