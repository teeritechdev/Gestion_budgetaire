package com.budget.backend.workflow.controller;

import com.budget.backend.common.PageResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.budget.backend.stock.service.AuditHelper;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.workflow.dto.*;
import com.budget.backend.workflow.service.WorkflowDefinitionService;
import com.budget.backend.workflow.service.WorkflowEngineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WorkflowController {

    @Autowired
    private WorkflowDefinitionService definitionService;
    @Autowired
    private WorkflowEngineService engineService;
    @Autowired
    private AuditHelper auditHelper;
    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping("/definitions")
    @PreAuthorize("hasPermission(null, 'workflow.lire')")
    public ResponseEntity<PageResponseDto<WorkflowDefinitionResponseDto>> definitions(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Boolean actif,
            @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        return ResponseEntity.ok(definitionService.filtrer(code, actif, pageable));
    }

    @GetMapping("/definitions/{id}")
    @PreAuthorize("hasPermission(null, 'workflow.lire')")
    public ResponseEntity<WorkflowDefinitionResponseDto> definition(@PathVariable UUID id) {
        return definitionService.trouverParId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasPermission(null, 'workflow.creer')")
    public ResponseEntity<WorkflowDefinitionResponseDto> creerDefinition(@Valid @RequestBody WorkflowDefinitionRequestDto dto) {
        WorkflowDefinitionResponseDto cree = definitionService.creer(dto);
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.INSERTION, "workflow_definitions", cree.getId(), null,
                AuditHelper.valeurs("code", cree.getCode(), "libelle", cree.getLibelle(), "nbEtapes", cree.getEtapes().size()));
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @GetMapping("/instances/{dossierId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkflowInstanceResponseDto> instance(@PathVariable UUID dossierId) {
        return engineService.trouverParDossier(dossierId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /** Décision (APPROVE ou REJECT) sur l'étape courante ; la permission requise est celle de l'étape, vérifiée par le moteur. */
    @PostMapping("/instances/execute")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkflowInstanceResponseDto> executer(@Valid @RequestBody WorkflowExecutionActionDto dto) {
        return ResponseEntity.ok(engineService.executer(dto));
    }
}
