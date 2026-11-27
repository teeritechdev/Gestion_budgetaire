package com.budget.backend.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Jeton de réinitialisation du mot de passe, à usage unique et de courte durée.
 * Seule son empreinte SHA-256 est conservée : le jeton lui-même n'est jamais stocké.
 */
@Entity
@Table(name = "jetons_reinitialisation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JetonReinitialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "utilisateur_id", nullable = false)
    private UUID utilisateurId;

    @Column(name = "empreinte", nullable = false, unique = true, length = 64)
    private String empreinte;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "utilise_le")
    private Instant utiliseLe;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;
}
