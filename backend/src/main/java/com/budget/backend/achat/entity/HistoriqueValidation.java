package com.budget.backend.achat.entity;

import com.budget.backend.security.entity.Utilisateur;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historique_validations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HistoriqueValidation {

    public static final String VALIDE = "VALIDE";
    public static final String REJETE = "REJETE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "da_id", nullable = false)
    private DemandeAchat demandeAchat;

    @Column(nullable = false, length = 50)
    private String etape;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valideur_id", nullable = false)
    private Utilisateur valideur;

    @Column(nullable = false, length = 30)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @CreationTimestamp
    @Column(name = "date_decision", updatable = false)
    private LocalDateTime dateDecision;
}
