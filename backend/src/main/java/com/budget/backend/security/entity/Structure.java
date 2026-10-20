package com.budget.backend.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "structures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Structure {

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
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Structure parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direction_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Structure direction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entite_code", referencedColumnName = "code", nullable = false)
    private Entite entite;

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
}
