package com.budget.backend.stock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DemandeMagasinServieDto {
    @NotEmpty
    @Valid
    private List<LigneServie> lignes;

    /** Quantité remise lors de CE service (s'ajoute à la quantité déjà servie). */
    @Data
    public static class LigneServie {
        @NotNull
        private UUID ligneId;
        @NotNull
        @PositiveOrZero
        private Integer quantiteServie;
    }
}
