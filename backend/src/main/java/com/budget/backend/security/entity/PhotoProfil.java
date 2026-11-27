package com.budget.backend.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Photo de profil d'un compte utilisateur (une par compte), stockée en base. */
@Entity
@Table(name = "photos_profil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoProfil {

    @Id
    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "contenu", nullable = false, columnDefinition = "bytea")
    private byte[] contenu;

    @Column(name = "type_mime", nullable = false, length = 30)
    private String typeMime;

    @Column(name = "taille", nullable = false)
    private Integer taille;

    @Column(name = "modifiee_le", nullable = false)
    private Instant modifieeLe;
}
