package com.budget.backend.achat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowExecutionActionDto {
    @NotBlank
    private String action; // APPROVE ou REJECT
    private String commentaire;
}
