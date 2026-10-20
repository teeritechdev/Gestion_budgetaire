package com.budget.backend.budget.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class LigneRevisionRequestDto {
    private String typeMouvement; // VIREMENT_INTERNE, EXTENSION
    private BigDecimal montant;
    private UUID allocationSourceId;
    private UUID allocationDestinationId;
}