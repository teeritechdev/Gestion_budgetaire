package com.budget.backend.budget.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AllocationOsRequestDto {
    private UUID exerciceId;
    private UUID osId;
    private BigDecimal montantAlloue;
}