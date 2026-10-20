package com.budget.backend.budget.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ActiviteCatalogueRequestDto {
    private UUID opId;
    private String codeActivite;
    private String libelle;
    private String description;
}