package com.budget.backend.budget.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExecutionStructureUpdateRequestDto {
    private BigDecimal montantEngage;
    private BigDecimal montantLiquide;
}