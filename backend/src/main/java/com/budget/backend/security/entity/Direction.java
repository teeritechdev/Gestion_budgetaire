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

    /** Structure de rattachement (une seule par direction). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_code", referencedColumnName = "code", nullable = false)
    private Structure structure;

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

    /** Code de la structure de rattachement. */
    public String codeStructure() {
        return structure == null ? null : structure.getCode();
    }

    public boolean estRattacheeA(String structureCode) {
        return structure != null && structure.getCode().equalsIgnoreCase(structureCode);
    }
}
