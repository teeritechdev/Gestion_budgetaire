package com.budget.backend.achat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fournisseurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Fournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_fournisseur", unique = true, nullable = false, length = 50)
    private String codeFournisseur; // Ex: FOURN-2026-0012

    @Column(name = "raison_sociale", nullable = false, length = 200)
    private String raisonSociale;

    @Column(unique = true, nullable = false, length = 50)
    private String ifu; // Identifiant Fiscal Unique

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String telephone;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Builder.Default
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
