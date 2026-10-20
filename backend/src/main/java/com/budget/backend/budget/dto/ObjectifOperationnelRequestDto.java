package com.budget.backend.budget.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ObjectifOperationnelRequestDto {
    private UUID osId;
    private String codeOp;
    private String libelle;
    private String description;
}