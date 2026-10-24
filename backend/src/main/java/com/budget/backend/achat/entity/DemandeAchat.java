package com.budget.backend.achat.entity;

import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "demandes_achat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DemandeAchat {

    public static final String BROUILLON = "BROUILLON";
    public static final String SOUMIS = "SOUMIS";
    public static final String EN_COURS_VALIDATION = "EN_COURS_VALIDATION";
    public static final String APPROUVE = "APPROUVE";
    public static final String REJETE = "REJETE";
    public static final String COMMANDE = "COMMANDE";

    // Etape de la DA approuvée, en attente de traitement par l'acheteur (hors circuit de validation)
    public static final String ETAPE_TRAITEMENT_CSP = "TRAITEMENT_CSP";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_dossier", unique = true, nullable = false, length = 50)
    private String codeDossier; // Ex: DA-2026-0045

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entite_emettrice", referencedColumnName = "code", nullable = false)
    private Entite entiteEmettrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entite_beneficiaire", referencedColumnName = "code", nullable = false)
    private Entite entiteBeneficiaire;

    @Column(name = "type_achat", nullable = false, length = 30)
    private String typeAchat; // STOCKABLE, PRESTATION_SERVICE, INVESTISSEMENT_IMMO, TRAVAUX

    @Column(nullable = false, length = 255)
    private String objet;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Builder.Default
    @Column(name = "montant_estime_ht", precision = 15, scale = 2)
    private BigDecimal montantEstimeHt = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "montant_estime_ttc", precision = 15, scale = 2)
    private BigDecimal montantEstimeTtc = BigDecimal.ZERO;

    @Builder.Default
    @Column(length = 30)
    private String statut = SOUMIS;

    @Column(name = "etape_courante", length = 50)
    private String etapeCourante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Utilisateur demandeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direction_demandeur_id", nullable = false)
    private Structure directionDemandeur;

    @Column(name = "exercice_annee", nullable = false)
    private Integer exerciceAnnee;

    @Builder.Default
    @OneToMany(mappedBy = "demandeAchat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneDemandeAchat> lignes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "demandeAchat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DossierVentilationImpact> ventilations = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "demandeAchat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateDecision ASC")
    private List<HistoriqueValidation> historique = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
