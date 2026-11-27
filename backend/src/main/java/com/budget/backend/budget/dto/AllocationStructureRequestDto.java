package com.budget.backend.budget.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AllocationStructureRequestDto {
    private UUID allocationActiviteId;
    private String structureCode;
    private BigDecimal montantVote;
}