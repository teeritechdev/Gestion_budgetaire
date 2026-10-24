package com.budget.backend.stock.entity;

import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "demandes_magasin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DemandeMagasin {

    public static final String SOUMIS = "SOUMIS";
    public static final String VALIDE_N1 = "VALIDE_N1";
    public static final String SERVIE = "SERVIE";
    public static final String PARTIELLEMENT_SERVIE = "PARTIELLEMENT_SERVIE";
    public static final String REJETEE = "REJETEE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_demande", unique = true, nullable = false, length = 50)
    private String numeroDemande; // Ex: BS-2026-0042

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Utilisateur demandeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Structure service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entite_code", referencedColumnName = "code", nullable = false)
    private Entite entite;

    @Column(nullable = false, length = 255)
    private String objet;

    @Builder.Default
    @Column(length = 30)
    private String statut = SOUMIS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valideur_n1_id")
    private Utilisateur valideurN1;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magasinier_id")
    private Utilisateur magasinier;

    @Column(name = "date_servie")
    private LocalDateTime dateServie;

    @Builder.Default
    @OneToMany(mappedBy = "demandeMagasin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneDemandeMagasin> lignes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
