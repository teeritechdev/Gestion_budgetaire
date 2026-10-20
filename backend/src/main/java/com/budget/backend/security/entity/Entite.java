package com.budget.backend.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "entites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Entite {

    @Id
    @Column(name = "code", length = 20, nullable = false)
    private String code; // Ex: UAB_VIE, UAB_HOLDING

    @Column(name = "nom", length = 150, nullable = false)
    private String nom; // Raison sociale complète

    @Column(name = "est_csp")
    @Builder.Default
    private Boolean estCsP = false; // Centre de Services Partagés ou filiale standard

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}