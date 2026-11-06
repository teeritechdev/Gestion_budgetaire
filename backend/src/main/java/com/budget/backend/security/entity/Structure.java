package com.budget.backend.security.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

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
    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "nom", length = 150, nullable = false)
    private String nom;


    @Column(length = 30)
    private String telephone;

    @Column(length = 255)
    private String adresse;

    @Column(length = 150)
    private String email;

    @Column(length = 100)
    private String ville;

    @Column(length = 100)
    private String pays;

    @Column(length = 30)
    private String sigle;

    @Column(name = "secteur_activite", length = 100)
    private String secteurActivite;

    @Column(name = "forme_juridique", length = 100)
    private String formeJuridique;

    @Column(name = "numero_fiscal", length = 100)
    private String numeroFiscal;

    @Column(name = "site_web", length = 255)
    private String siteWeb;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    @JsonProperty("libelle")
    public String getLibelle() {
        return nom;
    }
}