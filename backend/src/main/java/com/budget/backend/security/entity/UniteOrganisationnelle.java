package com.budget.backend.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "unites_organisationnelles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UniteOrganisationnelle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String code; // Ex: DIR-IT-01

    @Column(nullable = false, length = 150)
    private String libelle; // Ex: DSI

    @Column(name = "type_structure", nullable = false, length = 30)
    private String typeStructure; // DIRECTION, DEPARTEMENT, SERVICE, AGENCE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private UniteOrganisationnelle parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direction_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private UniteOrganisationnelle direction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_code", referencedColumnName = "code", nullable = false)
    private Structure structure;

    @Column(name = "responsable_id")
    private UUID responsableId; // ID de l'utilisateur responsable

    @Builder.Default
    @Column(nullable = false)
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    @JsonProperty(value = "parentId", access = JsonProperty.Access.READ_ONLY)
    public UUID getParentId() {
        return parent != null ? parent.getId() : null;
    }

    @Transient
    @JsonProperty(value = "directionId", access = JsonProperty.Access.READ_ONLY)
    public UUID getDirectionId() {
        return direction != null ? direction.getId() : null;
    }
}
