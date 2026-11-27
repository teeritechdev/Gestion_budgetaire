package com.budget.backend.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockKpisDto {
    private long totalArticles;
    private long articlesEnAlerte;
    private BigDecimal valeurTotaleStock;
    private long demandesEnAttenteValidation;
    private long demandesEnAttenteService;
}

