package com.budget.backend.achat.entity;

import com.budget.backend.security.entity.ServiceOrganisationnel;
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_dossier", unique = true, nullable = false, length = 50)
    private String codeDossier;

    @Column(name = "entite_emettrice", nullable = false, length = 20)
    private String entiteEmettrice;

    @Column(name = "entite_beneficiaire", nullable = false, length = 20)
    private String entiteBeneficiaire;

    @Column(name = "type_achat", nullable = false, length = 30)
    private String typeAchat;

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
    @Column(nullable = false, length = 30)
    private String statut = SOUMIS;

    @Builder.Default
    @Column(name = "etape_courante", length = 50)
    private String etapeCourante = "VALIDATION_N1";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Utilisateur demandeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_demandeur_id")
    private ServiceOrganisationnel serviceDemandeur;

    @Column(name = "exercice_annee", nullable = false)
    private Integer exerciceAnnee;

    @Builder.Default
    @OneToMany(mappedBy = "demandeAchat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneDemandeAchat> lignes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void recalculerMontants() {
        BigDecimal totalHt = BigDecimal.ZERO;
        if (lignes != null) {
            for (LigneDemandeAchat l : lignes) {
                if (l.getMontantTotalHt() != null) {
                    totalHt = totalHt.add(l.getMontantTotalHt());
                }
            }
        }
        this.montantEstimeHt = totalHt;
        if (this.montantEstimeTtc == null || this.montantEstimeTtc.compareTo(BigDecimal.ZERO) == 0) {
            this.montantEstimeTtc = totalHt;
        }
    }
}

