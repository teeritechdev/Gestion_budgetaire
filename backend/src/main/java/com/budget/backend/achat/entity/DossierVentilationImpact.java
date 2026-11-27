package com.budget.backend.achat.entity;

import com.budget.backend.budget.entity.ActiviteCatalogue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "da_id", nullable = false)
    private DemandeAchat demandeAchat;

    @Column(name = "entite_impactee", nullable = false, length = 20)
    private String entiteImpactee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activite_impactee_id")
    private ActiviteCatalogue activiteImpactee;

    @Column(name = "montant_impute", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantImpute;

    @Column(name = "pourcentage_applique", nullable = false, precision = 5, scale = 2)
    private BigDecimal pourcentageApplique;

    @Builder.Default
    @Column(name = "statut_blocage")
    private Boolean statutBlocage = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

