package com.budget.backend.achat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionValidationDto {

    @NotBlank(message = "La décision est obligatoire (ex: APPROUVE, REJETE)")
    private String decision;

    private String commentaire;

    private String etape;
}

