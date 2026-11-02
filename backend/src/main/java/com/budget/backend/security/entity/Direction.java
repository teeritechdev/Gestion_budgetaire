package com.budget.backend.security.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "directions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Direction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String libelle;

    /**
     * Structure de rattachement d'une direction classique.
     * Une direction CSP sert toutes les structures du groupe : elle n'en a aucune (null).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_code", referencedColumnName = "code")
    private Structure structure;

    /** Direction jouant le rôle de centre de services partagés (CSP) pour toutes les structures. */
    @Builder.Default
    @Column(name = "est_csp", nullable = false, columnDefinition = "boolean not null default false")
    private Boolean estCsp = false;

    @Column(name = "responsable_id")
    private UUID responsableId;

    @Builder.Default
    @Column(nullable = false)
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Code de la structure de rattachement, ou null pour une direction CSP transversale. */
    public String codeStructure() {
        return structure == null ? null : structure.getCode();
    }
}
