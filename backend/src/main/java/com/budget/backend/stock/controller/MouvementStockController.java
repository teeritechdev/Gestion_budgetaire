package com.budget.backend.stock.controller;

import com.budget.backend.common.PageResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.budget.backend.stock.StockException;
import com.budget.backend.stock.dto.MouvementStockCreateDto;
import com.budget.backend.stock.dto.MouvementStockResponseDto;
import com.budget.backend.stock.entity.MouvementStock;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.StockMovementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/mouvements-stock")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MouvementStockController {

    @Autowired
    private StockMovementService service;

    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'mouvement_stock.lire')")
    public ResponseEntity<PageResponseDto<MouvementStockResponseDto>> listerOuFiltrer(
            @RequestParam(required = false) UUID articleId,
            @RequestParam(required = false) String typeMouvement,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(articleId, typeMouvement, dateDebut, dateFin, pageable));
    }

    /** Saisie manuelle : entrée hors réception, sortie hors bon de sortie, ou ajustement d'inventaire. */
    @PostMapping
    @PreAuthorize("hasPermission(null, 'mouvement_stock.creer')")
    public ResponseEntity<MouvementStockResponseDto> creer(@Valid @RequestBody MouvementStockCreateDto dto) {
        var auteur = currentUserService.utilisateurConnecte();
        String type = dto.getTypeMouvement().trim().toUpperCase();
        MouvementStock mouvement = switch (type) {
            case MouvementStock.ENTREE -> service.entreeStock(dto.getArticleId(), dto.getQuantite(),
                    dto.getPrixUnitaire(), dto.getReferenceDocument(), auteur);
            case MouvementStock.SORTIE -> service.sortieStock(dto.getArticleId(), dto.getQuantite(),
                    dto.getReferenceDocument(), auteur);
            case MouvementStock.AJUSTEMENT -> service.ajusterStock(dto.getArticleId(), dto.getQuantite(),
                    dto.getReferenceDocument(), auteur);
            default -> throw new StockException("Type de mouvement invalide : '" + dto.getTypeMouvement()
                    + "' (attendu : ENTREE, SORTIE ou AJUSTEMENT).");
        };
        return new ResponseEntity<>(MouvementStockResponseDto.from(mouvement), HttpStatus.CREATED);
    }
}
