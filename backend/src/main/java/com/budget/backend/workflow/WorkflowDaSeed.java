package com.budget.backend.workflow;

import com.budget.backend.workflow.api.WorkflowEngineApi;
import com.budget.backend.workflow.dto.WorkflowDefinitionRequestDto;
import com.budget.backend.workflow.dto.WorkflowEtapeRequestDto;
import com.budget.backend.workflow.repository.WorkflowDefinitionRepository;
import com.budget.backend.workflow.service.WorkflowDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Crée au démarrage le workflow des demandes d'achat (Module 4) s'il n'existe pas, avec le circuit du MRD
 * (historique_validations) : validation N+1, contrôle budgétaire, puis validation DG pour les montants
 * supérieurs à 50 000 (seuil de l'exemple du PDF : étape DG à partir de 50 000,01). Ensuite il se gère
 * par POST /api/workflows/definitions.
 */
@Component
@Order(3)
public class WorkflowDaSeed implements CommandLineRunner {

    @Autowired
    private WorkflowDefinitionRepository repository;
    @Autowired
    private WorkflowDefinitionService definitionService;

    @Override
    public void run(String... args) {
        if (repository.existsByCode(WorkflowEngineApi.WORKFLOW_DA)) {
            return;
        }
        WorkflowDefinitionRequestDto dto = new WorkflowDefinitionRequestDto();
        dto.setCode(WorkflowEngineApi.WORKFLOW_DA);
        dto.setLibelle("Circuit de validation d'une demande d'achat");
        dto.setActif(true);
        dto.setEtapes(List.of(
                etape(1, "VALIDATION_N1", "Validation par le responsable hiérarchique (N+1)", "demande_achat.viser",
                        null, "CONTROLE_BUDGETAIRE"),
                etape(2, "CONTROLE_BUDGETAIRE", "Contrôle de disponibilité budgétaire", "demande_achat.controler_budget",
                        null, "VALIDATION_DG"),
                etape(3, "VALIDATION_DG", "Approbation de la Direction Générale", "demande_achat.approuver_dg",
                        new BigDecimal("50000.01"), null)));
        definitionService.creer(dto);
        System.out.println("--> [WORKFLOW SEED] Workflow " + WorkflowEngineApi.WORKFLOW_DA + " créé (3 étapes).");
    }

    private WorkflowEtapeRequestDto etape(int num, String code, String libelle, String permission, BigDecimal min, String succes) {
        WorkflowEtapeRequestDto e = new WorkflowEtapeRequestDto();
        e.setNumEtape(num);
        e.setCodeEtape(code);
        e.setLibelle(libelle);
        e.setPermissionRequise(permission);
        e.setMontantMinimum(min);
        e.setEtapeSuivanteSuccesCode(succes);
        return e;
    }
}
