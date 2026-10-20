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
@Table(name = "repartition_csp_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RepartitionCspRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "activite_csp_id", nullable = false)
    private UUID activiteCspId;

    @Column(name = "entite_beneficiaire", nullable = false, length = 50)
    private String entiteBeneficiaire;

    @Column(name = "activite_beneficiaire_id", nullable = false)
    private UUID activiteBeneficiaireId;

    @Column(name = "cle_type", length = 50)
    private String cleType; // ex: EFFECTIF, CHIFFRE_AFFAIRES

    @Column(name = "valeur_cle", precision = 15, scale = 4)
    private BigDecimal valeurCle;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}