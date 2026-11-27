package com.budget.backend.stock.controller;

import com.budget.backend.common.PageResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.budget.backend.stock.dto.ArticleRequestDto;
import com.budget.backend.stock.dto.ArticleResponseDto;
import com.budget.backend.stock.entity.Article;
import com.budget.backend.stock.service.ArticleImportService;
import com.budget.backend.stock.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ArticleController {

    @Autowired
    private ArticleService service;

    @Autowired
    private ArticleImportService importService;

    @GetMapping("/kpis")
    @PreAuthorize("hasPermission(null, 'article.lire')")
    public ResponseEntity<com.budget.backend.stock.dto.StockKpisDto> obtenirKpis() {
        return ResponseEntity.ok(service.obtenirKpis());
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'article.lire')")
    public ResponseEntity<PageResponseDto<ArticleResponseDto>> listerOuFiltrer(
            @RequestParam(required = false) String codeArticle,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) Boolean sousSeuil,
            @PageableDefault(size = 20, sort = "codeArticle") Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(codeArticle, designation, sousSeuil, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'article.lire')")
    public ResponseEntity<ArticleResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'article.creer')")
    public ResponseEntity<ArticleResponseDto> creer(@Valid @RequestBody ArticleRequestDto dto) {
        return new ResponseEntity<>(service.creer(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'article.modifier')")
    public ResponseEntity<ArticleResponseDto> modifier(@PathVariable UUID id, @Valid @RequestBody ArticleRequestDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @PostMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'article.lire')")
    public ResponseEntity<byte[]> exporterCsv(@RequestBody(required = false) Map<String, List<UUID>> body) {
        List<UUID> ids = body != null ? body.get("ids") : null;
        List<Article> articles = service.listerTous(ids);
        byte[] csvBytes = service.exporterEnCsv(articles).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"articles_" + LocalDate.now() + ".csv\"")
                .body(csvBytes);
    }

    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission(null, 'article.creer')")
    public ResponseEntity<Map<String, Object>> importerCsv(@RequestParam("file") MultipartFile file) {
        int count = importService.importerArticles(file);
        return ResponseEntity.ok(Map.of(
                "message", count + " article(s) importé(s) ou mis à jour avec succès.",
                "elementsImportes", count));
    }
}
