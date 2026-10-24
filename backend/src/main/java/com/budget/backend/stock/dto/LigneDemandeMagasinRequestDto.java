package com.budget.backend.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class LigneDemandeMagasinRequestDto {
    @NotNull
    private UUID articleId;
    @NotNull
    @Positive
    private Integer quantiteDemande;
}
