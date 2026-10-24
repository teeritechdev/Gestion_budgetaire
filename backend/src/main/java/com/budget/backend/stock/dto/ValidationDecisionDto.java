package com.budget.backend.stock.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidationDecisionDto {
    @NotBlank
    private String decision; // APPROUVE ou REJETE
    private String commentaire;
}
