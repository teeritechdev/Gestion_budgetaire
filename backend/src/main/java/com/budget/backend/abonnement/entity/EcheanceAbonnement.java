package com.budget.backend.abonnement.entity;

import com.budget.backend.achat.entity.BonCommande;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "echeances_abonnement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EcheanceAbonnement {

    public static final String A_VENIR = "A_VENIR";
    public static final String BC_GENERE = "BC_GENERE";
    public static final String FACTURE_RECUE = "FACTURE_RECUE";
    public static final String PAYE = "PAYE";
    public static final String ANNULE = "ANNULE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "abonnement_id", nullable = false)
    private Abonnement abonnement;

    @Column(name = "num_echeance", nullable = false)
    private Integer numEcheance;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "montant_attendu_ttc", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantAttenduTtc;

    @Column(name = "montant_facture_ttc", precision = 15, scale = 2)
    private BigDecimal montantFactureTtc;

    @Builder.Default
    @Column(length = 20)
    private String statut = A_VENIR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bc_id")
    private BonCommande bonCommande;

    @Column(name = "reference_facture", length = 100)
    private String referenceFacture;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Ecart facturé - attendu (positif = surconsommation) ; null tant qu'aucune facture n'est saisie. */
    public BigDecimal ecartTtc() {
        return montantFactureTtc == null ? null : montantFactureTtc.subtract(montantAttenduTtc);
    }
}
