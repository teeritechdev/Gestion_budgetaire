package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.DemandeRevisionRequestDto;
import com.budget.backend.budget.dto.ValidationRevisionRequestDto;
import com.budget.backend.budget.entity.DemandeRevisionBudgetaire;
import com.budget.backend.budget.entity.LigneRevisionBudgetaire;
import com.budget.backend.budget.service.RevisionBudgetaireService;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.security.CustomPermissionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/revisions-budgetaires")
public class RevisionBudgetaireController {

    private static final String PERMISSION_VALIDER = "revision.valider";

    @Autowired
    private RevisionBudgetaireService service;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private CustomPermissionEvaluator permissionEvaluator;

    // --- ENDPOINTS DEMANDES DE RÉVISION ---

    /**
     * Le demandeur voit ses propres demandes ; un valideur voit en plus les demandes
     * soumises ou décidées des autres utilisateurs (jamais leurs brouillons).
     */
    @GetMapping
    @PreAuthorize("hasPermission(null, 'revision.lire')")
    public ResponseEntity<List<DemandeRevisionBudgetaire>> listerOuFiltrer(
            @RequestParam(required = false) Integer anneeExercice,
            @RequestParam(required = false) String statut,
            Authentication authentication) {
        UUID utilisateurId = authenticatedUserId(authentication);
        return ResponseEntity.ok(service.listerVisibles(
                utilisateurId, peutValider(authentication), anneeExercice, statut));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'revision.lire')")
    public ResponseEntity<DemandeRevisionBudgetaire> obtenirParId(
            @PathVariable UUID id, Authentication authentication) {
        return service.trouverVisible(id, authenticatedUserId(authentication), peutValider(authentication))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'revision.creer')")
    public ResponseEntity<DemandeRevisionBudgetaire> creer(
            @RequestBody DemandeRevisionRequestDto dto, Authentication authentication) {
        DemandeRevisionBudgetaire demande = DemandeRevisionBudgetaire.builder()
                .numeroDemande(dto.getNumeroDemande())
                .anneeExercice(dto.getAnneeExercice())
                .demandeurId(authenticatedUserId(authentication))
                .motif(dto.getMotif())
                .build();
        DemandeRevisionBudgetaire cree = service.creerDemandeAvecLignes(demande, dto.getLignes());
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'revision.modifier')")
    public ResponseEntity<DemandeRevisionBudgetaire> modifier(
            @PathVariable UUID id, @RequestBody DemandeRevisionRequestDto dto, Authentication authentication) {
        UUID demandeurId = authenticatedUserId(authentication);
        if (service.trouverParIdEtDemandeur(id, demandeurId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DemandeRevisionBudgetaire modifications = DemandeRevisionBudgetaire.builder()
                .numeroDemande(dto.getNumeroDemande())
                .anneeExercice(dto.getAnneeExercice())
                .demandeurId(demandeurId)
                .motif(dto.getMotif())
                .build();
        return ResponseEntity.ok(service.modifierDemandeAvecLignes(id, modifications, dto.getLignes()));
    }

    @PutMapping("/{id}/soumettre")
    @PreAuthorize("hasPermission(null, 'revision.soumettre')")
    public ResponseEntity<DemandeRevisionBudgetaire> soumettre(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(service.soumettreDemande(id, authenticatedUserId(authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'revision.supprimer')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id, Authentication authentication) {
        service.supprimerDemandeBrouillon(id, authenticatedUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID authenticatedUserId(Authentication authentication) {
        return utilisateurRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Utilisateur authentifié introuvable."))
                .getId();
    }

    private boolean peutValider(Authentication authentication) {
        return permissionEvaluator.hasPermission(authentication, null, PERMISSION_VALIDER);
    }

    /** Le valideur est toujours l'utilisateur authentifié : un éventuel valideurId du corps est ignoré. */
    @PutMapping("/{id}/valider")
    @PreAuthorize("hasPermission(null, 'revision.valider')")
    public ResponseEntity<DemandeRevisionBudgetaire> valider(
            @PathVariable UUID id, @RequestBody ValidationRevisionRequestDto dto, Authentication authentication) {
        DemandeRevisionBudgetaire decision = service.validerDemande(
                id, authenticatedUserId(authentication), dto.getStatut(), dto.getCommentaire());
        return ResponseEntity.ok(decision);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'revision.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) Integer anneeExercice,
            @RequestParam(required = false) String statut,
            Authentication authentication) {
        byte[] bytes = service.exporterEnCsv(anneeExercice, statut,
                authenticatedUserId(authentication), peutValider(authentication)).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "revisions_budgetaires_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'revision.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) Integer anneeExercice,
            @RequestParam(required = false) String statut,
            Authentication authentication) {
        byte[] pdfBytes = service.exporterEnPdf(anneeExercice, statut,
                authenticatedUserId(authentication), peutValider(authentication));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "revisions_budgetaires_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // --- ENDPOINTS DÉTAILS LIGNES DE RÉVISION ---

    @GetMapping("/{id}/lignes")
    @PreAuthorize("hasPermission(null, 'revision.lire')")
    public ResponseEntity<List<LigneRevisionBudgetaire>> obtenirLignesParDemande(
            @PathVariable UUID id, Authentication authentication) {
        if (service.trouverVisible(id, authenticatedUserId(authentication), peutValider(authentication)).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.listerLignesParDemande(id));
    }

    @GetMapping("/lignes")
    @PreAuthorize("hasPermission(null, 'revision.lire')")
    public ResponseEntity<List<LigneRevisionBudgetaire>> listerOuFiltrerLignes(
            @RequestParam(required = false) UUID demandeId,
            @RequestParam(required = false) String typeMouvement,
            @RequestParam(required = false) UUID allocationSourceId,
            @RequestParam(required = false) UUID allocationDestinationId,
            Authentication authentication) {
        return ResponseEntity.ok(service.filtrerLignes(demandeId, typeMouvement, allocationSourceId,
                allocationDestinationId, authenticatedUserId(authentication), peutValider(authentication)));
    }

    @GetMapping("/lignes/export/csv")
    @PreAuthorize("hasPermission(null, 'revision.exporter')")
    public ResponseEntity<byte[]> exporterLignesCsv(
            @RequestParam(required = false) UUID demandeId,
            @RequestParam(required = false) String typeMouvement,
            @RequestParam(required = false) UUID allocationSourceId,
            @RequestParam(required = false) UUID allocationDestinationId,
            Authentication authentication) {
        byte[] bytes = service.exporterLignesCsv(demandeId, typeMouvement, allocationSourceId, allocationDestinationId,
                authenticatedUserId(authentication), peutValider(authentication)).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "lignes_revision_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/lignes/export/pdf")
    @PreAuthorize("hasPermission(null, 'revision.exporter')")
    public ResponseEntity<byte[]> exporterLignesPdf(
            @RequestParam(required = false) UUID demandeId,
            @RequestParam(required = false) String typeMouvement,
            @RequestParam(required = false) UUID allocationSourceId,
            @RequestParam(required = false) UUID allocationDestinationId,
            Authentication authentication) {
        byte[] pdfBytes = service.exporterLignesPdf(demandeId, typeMouvement, allocationSourceId,
                allocationDestinationId, authenticatedUserId(authentication), peutValider(authentication));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "lignes_revision_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> gererErreurMetier(ResponseStatusException exception) {
        String message = exception.getReason() == null
                ? "L'opération sur la demande de révision a échoué."
                : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", message));
    }
}
