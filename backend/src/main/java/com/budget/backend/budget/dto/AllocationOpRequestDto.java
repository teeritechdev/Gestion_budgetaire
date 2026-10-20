package com.budget.backend.budget.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AllocationOpRequestDto {
    private UUID allocationOsId;
    private UUID opId;
    private BigDecimal montantAlloue;
}