package com.budget.backend.budget.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AllocationActiviteRequestDto {
    private UUID allocationOpId;
    private UUID activiteCatalogueId;
    private BigDecimal montantAlloue;
}