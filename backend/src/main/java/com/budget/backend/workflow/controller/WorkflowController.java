package com.budget.backend.workflow.controller;

import com.budget.backend.achat.dto.HistoriqueValidationResponseDto;
import com.budget.backend.common.PageResponseDto;
import com.budget.backend.workflow.dto.*;
import com.budget.backend.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    private WorkflowService service;

    // =========================================================================
    // 1. MODÈLES DE WORKFLOW (workflow_definitions)
    // =========================================================================

    @GetMapping
    @PreAuthorize("hasPermission(null, 'workflow.lire')")
    public ResponseEntity<PageResponseDto<WorkflowDefinitionResponseDto>> listerDefinitions(
            @RequestParam(required = false) String typeEntite,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche,
            @PageableDefault(size = 20, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrerDefinitions(typeEntite, actif, recherche, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'workflow.lire')")
    public ResponseEntity<WorkflowDefinitionResponseDto> obtenirDefinitionParId(@PathVariable UUID id) {
        return service.trouverDefinitionParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'workflow.creer')")
    public ResponseEntity<WorkflowDefinitionResponseDto> creerDefinition(@Valid @RequestBody WorkflowDefinitionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerDefinition(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'workflow.modifier')")
    public ResponseEntity<WorkflowDefinitionResponseDto> modifierDefinition(
            @PathVariable UUID id,
            @Valid @RequestBody WorkflowDefinitionRequestDto dto) {
        return ResponseEntity.ok(service.modifierDefinition(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'workflow.supprimer')")
    public ResponseEntity<Void> supprimerDefinition(@PathVariable UUID id) {
        service.supprimerDefinition(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 2. CONFIGURATION DES ÉTAPES DU WORKFLOW
    // =========================================================================

    @GetMapping("/{workflowId}/etapes")
    @PreAuthorize("hasPermission(null, 'workflow.lire')")
    public ResponseEntity<List<WorkflowEtapeDto>> listerEtapes(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(service.listerEtapes(workflowId));
    }

    @PostMapping("/{workflowId}/etapes")
    @PreAuthorize("hasPermission(null, 'workflow.modifier')")
    public ResponseEntity<WorkflowEtapeDto> enregistrerEtape(
            @PathVariable UUID workflowId,
            @Valid @RequestBody WorkflowEtapeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouterOuModifierEtape(workflowId, dto));
    }

    @DeleteMapping("/{workflowId}/etapes/{etapeId}")
    @PreAuthorize("hasPermission(null, 'workflow.modifier')")
    public ResponseEntity<Void> supprimerEtape(
            @PathVariable UUID workflowId,
            @PathVariable UUID etapeId) {
        service.supprimerEtape(workflowId, etapeId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 3. INSTANCES & CIRCUITS D'APPROBATION EN COURS
    // =========================================================================

    @GetMapping("/instances")
    @PreAuthorize("hasPermission(null, 'workflow.lire')")
    public ResponseEntity<PageResponseDto<WorkflowInstanceResponseDto>> listerInstances(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String typeEntite,
            @RequestParam(required = false) String recherche,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrerInstances(statut, typeEntite, recherche, pageable));
    }

    @GetMapping("/instances/{id}")
    @PreAuthorize("hasPermission(null, 'workflow.lire')")
    public ResponseEntity<WorkflowInstanceResponseDto> obtenirInstanceParId(@PathVariable UUID id) {
        return service.trouverInstanceParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/instances/dossier/{dossierId}")
    @PreAuthorize("hasPermission(null, 'workflow.lire')")
    public ResponseEntity<WorkflowInstanceResponseDto> obtenirInstanceParDossier(@PathVariable UUID dossierId) {
        return service.trouverInstanceParDossierId(dossierId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/instances/{id}/historique")
    @PreAuthorize("hasPermission(null, 'workflow.lire')")
    public ResponseEntity<List<HistoriqueValidationResponseDto>> obtenirHistorique(@PathVariable UUID id) {
        return ResponseEntity.ok(service.trouverHistoriqueInstance(id));
    }

    @GetMapping("/mes-validations")
    public ResponseEntity<List<WorkflowInstanceResponseDto>> mesValidations() {
        return ResponseEntity.ok(service.trouverMesValidationsEnAttente());
    }

    @PostMapping("/instances/{id}/decision")
    @PreAuthorize("hasPermission(null, 'workflow.valider')")
    public ResponseEntity<WorkflowInstanceResponseDto> traiterDecision(
            @PathVariable UUID id,
            @Valid @RequestBody WorkflowDecisionRequestDto dto) {
        return ResponseEntity.ok(service.traiterDecision(id, dto));
    }
}
