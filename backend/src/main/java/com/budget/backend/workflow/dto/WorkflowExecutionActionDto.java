package com.budget.backend.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class WorkflowExecutionActionDto {
    @NotNull
    private UUID dossierId;
    @NotBlank
    private String action; // APPROVE ou REJECT
    private String commentaire;
}
