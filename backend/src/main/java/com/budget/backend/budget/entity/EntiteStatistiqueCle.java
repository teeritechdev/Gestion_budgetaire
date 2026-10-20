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
@Table(name = "entites_statistiques_cles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EntiteStatistiqueCle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "annee_exercice", nullable = false)
    private Integer anneeExercice;

    private Integer mois; // 1 à 12

    @Column(name = "entite_code", nullable = false, length = 50)
    private String entiteCode;

    @Column(name = "effectif_agents")
    private Integer effectifAgents;

    @Column(name = "chiffre_affaires", precision = 15, scale = 2)
    private BigDecimal chiffreAffaires;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}