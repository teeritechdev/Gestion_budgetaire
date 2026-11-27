package com.budget.backend.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDecisionRequestDto {

    @NotBlank(message = "La décision est obligatoire (VALIDER ou REJETER)")
    private String decision; // VALIDER, REJETER

    private String commentaire;
}

