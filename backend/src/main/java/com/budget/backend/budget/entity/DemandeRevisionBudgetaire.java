package com.budget.backend.budget.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "demandes_revision_budgetaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DemandeRevisionBudgetaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_demande", unique = true, nullable = false, length = 50)
    private String numeroDemande;

    @Column(name = "annee_exercice", nullable = false)
    private Integer anneeExercice;

    @Column(name = "demandeur_id", nullable = false)
    private UUID demandeurId;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(nullable = false, length = 50)
    private String statut; // BROUILLON, EN_ATTENTE, VALIDE, REJETE

    @Column(name = "valideur_id")
    private UUID valideurId;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "commentaire_validation", columnDefinition = "TEXT")
    private String commentaireValidation;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}