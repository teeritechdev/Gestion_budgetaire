package com.budget.backend.security.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Affectation d'un salarié à une entité de l'organisation : une structure, une direction ou un service.
 * Un salarié peut avoir plusieurs affectations, éventuellement simultanées.
 * Exactement une des trois cibles est renseignée, celle qui correspond au niveau.
 */
@Entity
@Table(name = "affectations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salarie_id", nullable = false)
    private Salarie salarie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NiveauAffectation niveau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_code", referencedColumnName = "code")
    private Structure structure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direction_id")
    private Direction direction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceOrganisationnel service;

    /** Rôle tenu dans l'entité cible (ex. Directeur, Chef de service, Agent). */
    @Column(length = 100)
    private String fonction;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /** Fin de l'affectation (incluse) ; vide tant qu'elle est en cours. */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(length = 500)
    private String commentaire;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** En cours à la date donnée : commencée et pas encore terminée. */
    public boolean estEnCoursLe(LocalDate date) {
        return !dateDebut.isAfter(date) && (dateFin == null || !dateFin.isBefore(date));
    }

    /** Structure à laquelle l'affectation se rattache, quel que soit son niveau. */
    public Structure structureDeRattachement() {
        return switch (niveau) {
            case STRUCTURE -> structure;
            case DIRECTION -> direction == null ? null : direction.getStructure();
            case SERVICE -> service == null || service.getDirection() == null ? null : service.getDirection().getStructure();
        };
    }

    /** Direction concernée : la cible elle-même ou la direction du service ; null pour une structure. */
    public Direction directionDeRattachement() {
        return switch (niveau) {
            case STRUCTURE -> null;
            case DIRECTION -> direction;
            case SERVICE -> service == null ? null : service.getDirection();
        };
    }
}
