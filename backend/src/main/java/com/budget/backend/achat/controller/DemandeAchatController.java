package com.budget.backend.achat.controller;

import com.budget.backend.common.PageResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.budget.backend.achat.dto.DemandeAchatRequestDto;
import com.budget.backend.achat.dto.DemandeAchatResponseDto;
import com.budget.backend.achat.dto.WorkflowExecutionActionDto;
import com.budget.backend.achat.service.DemandeAchatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/demandes-achat")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DemandeAchatController {

    @Autowired
    private DemandeAchatService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'demande_achat.lire')")
    public ResponseEntity<PageResponseDto<DemandeAchatResponseDto>> listerOuFiltrer(
            @RequestParam(required = false) String codeDossier,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID demandeurId,
            @RequestParam(required = false) Integer exerciceAnnee,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(codeDossier, statut, demandeurId, exerciceAnnee, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'demande_achat.lire')")
    public ResponseEntity<DemandeAchatResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'demande_achat.creer')")
    public ResponseEntity<DemandeAchatResponseDto> creer(@Valid @RequestBody DemandeAchatRequestDto dto) {
        return new ResponseEntity<>(service.creer(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'demande_achat.modifier')")
    public ResponseEntity<DemandeAchatResponseDto> modifier(@PathVariable UUID id, @Valid @RequestBody DemandeAchatRequestDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    /** Une étape du circuit de validation : la permission requise dépend de l'étape courante du dossier. */
    @PostMapping("/{id}/approuver")
    @PreAuthorize("hasPermission(null, 'demande_achat.viser') or hasPermission(null, 'demande_achat.controler_budget') "
            + "or hasPermission(null, 'demande_achat.approuver_dg')")
    public ResponseEntity<DemandeAchatResponseDto> approuver(@PathVariable UUID id, @Valid @RequestBody WorkflowExecutionActionDto dto) {
        return ResponseEntity.ok(service.approuver(id, dto));
    }
}
