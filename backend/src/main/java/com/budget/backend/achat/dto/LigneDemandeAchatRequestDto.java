package com.budget.backend.achat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneDemandeAchatRequestDto {

    @NotNull(message = "L'activité budgétaire est obligatoire")
    private UUID activiteId;

    private UUID articleId;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    private String uniteMesure;

    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être strictement positive")
    private Integer quantite;

    @NotNull(message = "Le prix unitaire estimé est obligatoire")
    @Positive(message = "Le prix unitaire doit être positif")
    private BigDecimal prixUnitaireEstime;
}

