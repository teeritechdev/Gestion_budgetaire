package com.budget.backend.budget.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ValidationRevisionRequestDto {
    private UUID valideurId;
    private String commentaire;
}