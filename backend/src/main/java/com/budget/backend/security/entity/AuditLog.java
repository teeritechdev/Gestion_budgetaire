package com.budget.backend.security.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Column(name = "type_action", nullable = false, length = 50)
    private String typeAction; // CONNEXION, INSERTION, MODIFICATION, SUPPRESSION...

    @Column(name = "nom_table", nullable = false, length = 50)
    private String nomTable;

    @Column(name = "enregistrement_id", nullable = false, length = 100)
    private String enregistrementId;

    @Type(JsonType.class)
    @Column(name = "anciennes_valeurs", columnDefinition = "jsonb")
    private Map<String, Object> anciennesValeurs;

    @Type(JsonType.class)
    @Column(name = "nouvelles_valeurs", columnDefinition = "jsonb")
    private Map<String, Object> nouvellesValeurs;

    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
