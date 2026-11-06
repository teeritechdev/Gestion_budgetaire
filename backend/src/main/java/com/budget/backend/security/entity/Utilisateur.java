package com.budget.backend.security.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 150)
    private String username;

    @Column(name = "mot_de_passe_hash", nullable = false, length = 255)
    @JsonIgnore
    private String motDePasseHash;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salarie_id", nullable = false, unique = true)
    private Salarie salarie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private Boolean actif = true;

    /** Mot de passe attribué par un administrateur : à changer à la prochaine connexion. */
    @Builder.Default
    @Column(name = "doit_changer_mot_de_passe", nullable = false, columnDefinition = "boolean not null default false")
    private Boolean doitChangerMotDePasse = false;

    /** Dernier changement de mot de passe : les jetons de connexion émis avant sont refusés. */
    @Column(name = "mot_de_passe_modifie_le")
    private Instant motDePasseModifieLe;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
