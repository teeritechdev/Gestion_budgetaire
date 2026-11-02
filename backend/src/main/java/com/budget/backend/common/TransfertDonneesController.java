package com.budget.backend.common;

import com.budget.backend.budget.entity.DemandeRevisionBudgetaire;
import com.budget.backend.budget.repository.ActiviteCatalogueRepository;
import com.budget.backend.budget.repository.BudgetAllocationActiviteRepository;
import com.budget.backend.budget.repository.BudgetAllocationOpRepository;
import com.budget.backend.budget.repository.BudgetAllocationOsRepository;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.budget.repository.DemandeRevisionBudgetaireRepository;
import com.budget.backend.budget.repository.EntiteStatistiqueCleRepository;
import com.budget.backend.budget.repository.ExerciceBudgetaireRepository;
import com.budget.backend.budget.repository.JournalMouvementsBudgetaireRepository;
import com.budget.backend.budget.repository.ObjectifOperationnelRepository;
import com.budget.backend.budget.repository.ObjectifStrategiqueRepository;
import com.budget.backend.budget.repository.PlanStrategiqueRepository;
import com.budget.backend.budget.repository.RepartitionCspRuleRepository;
import com.budget.backend.budget.service.ActiviteCatalogueService;
import com.budget.backend.budget.service.BudgetAllocationStructureService;
import com.budget.backend.budget.service.BudgetExportService;
import com.budget.backend.budget.service.BudgetImportService;
import com.budget.backend.budget.service.CascadeAllocationService;
import com.budget.backend.budget.service.EntiteStatistiqueCleService;
import com.budget.backend.budget.service.ExerciceBudgetaireService;
import com.budget.backend.budget.service.JournalMouvementsBudgetaireService;
import com.budget.backend.budget.service.ObjectifOperationnelService;
import com.budget.backend.budget.service.ObjectifStrategiqueService;
import com.budget.backend.budget.service.PlanStrategiqueService;
import com.budget.backend.budget.service.RepartitionCspRuleService;
import com.budget.backend.budget.service.RevisionBudgetaireService;
import com.budget.backend.security.dto.DirectionResponseDto;
import com.budget.backend.security.dto.SalarieResponseDto;
import com.budget.backend.security.dto.ServiceResponseDto;
import com.budget.backend.security.dto.UtilisateurResponseDto;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.AuditLogRepository;
import com.budget.backend.security.repository.PermissionRepository;
import com.budget.backend.security.repository.RoleRepository;
import com.budget.backend.security.repository.StructureRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.security.CustomPermissionEvaluator;
import com.budget.backend.security.service.AuditLogService;
import com.budget.backend.security.service.AuditService;
import com.budget.backend.security.service.DirectionService;
import com.budget.backend.security.service.OrganisationImportService;
import com.budget.backend.security.service.PermissionService;
import com.budget.backend.security.service.RoleService;
import com.budget.backend.security.service.SalarieService;
import com.budget.backend.security.service.ServiceOrganisationnelService;
import com.budget.backend.security.service.StructureService;
import com.budget.backend.security.service.UtilisateurService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Exports et imports communs à tous les onglets.
 *
 * <p><b>Exports</b> : {@code POST /api/exports/{ressource}/{csv|pdf}} avec {@code {"ids": [...]}}. Le client
 * transmet les identifiants des lignes <i>affichées</i> (après ses filtres et son tri) : le fichier contient
 * exactement ces lignes, dans cet ordre. Les permissions et la visibilité sont revérifiées côté serveur.</p>
 *
 * <p><b>Imports</b> : {@code POST /api/imports/{ressource}} (multipart, champ {@code file}).</p>
 */
@RestController
@RequestMapping("/api")
public class TransfertDonneesController {

    private static final int MAX_LIGNES_EXPORT = 20_000;
    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /** Une ressource exportable : permission requise et génération CSV/PDF à partir des identifiants. */
    private record Export(String permission, String nomFichier,
                          BiFunction<List<String>, Authentication, String> csv,
                          BiFunction<List<String>, Authentication, byte[]> pdf) {}

    /** Une ressource importable : l'une des permissions listées suffit à accéder à l'import. */
    private record Import(List<String> permissions, BiFunction<MultipartFile, Authentication, Integer> traitement) {}

    public record SelectionExport(List<String> ids) {}

    private final Map<String, Export> exports = new HashMap<>();
    private final Map<String, Import> imports = new HashMap<>();
    private final CustomPermissionEvaluator permissionEvaluator;
    private final AuditService auditService;
    private final UtilisateurRepository utilisateurRepository;

    public TransfertDonneesController(
            CustomPermissionEvaluator permissionEvaluator,
            AuditService auditService,
            UtilisateurRepository utilisateurRepository,
            // Budget
            PlanStrategiqueService planService, PlanStrategiqueRepository planRepository,
            ObjectifStrategiqueService osService, ObjectifStrategiqueRepository osRepository,
            ObjectifOperationnelService opService, ObjectifOperationnelRepository opRepository,
            ActiviteCatalogueService activiteService, ActiviteCatalogueRepository activiteRepository,
            ExerciceBudgetaireService exerciceService, ExerciceBudgetaireRepository exerciceRepository,
            CascadeAllocationService cascadeService,
            BudgetAllocationOsRepository allocationOsRepository,
            BudgetAllocationOpRepository allocationOpRepository,
            BudgetAllocationActiviteRepository allocationActiviteRepository,
            BudgetAllocationStructureService allocationStructureService,
            BudgetAllocationStructureRepository allocationStructureRepository,
            EntiteStatistiqueCleService statistiqueService, EntiteStatistiqueCleRepository statistiqueRepository,
            RepartitionCspRuleService repartitionService, RepartitionCspRuleRepository repartitionRepository,
            RevisionBudgetaireService revisionService, DemandeRevisionBudgetaireRepository demandeRepository,
            JournalMouvementsBudgetaireService journalService, JournalMouvementsBudgetaireRepository journalRepository,
            BudgetImportService budgetImportService, BudgetExportService budgetExportService,
            // Sécurité et organisation
            AuditLogService auditLogService, AuditLogRepository auditLogRepository,
            StructureService structureService, StructureRepository structureRepository,
            DirectionService directionService, ServiceOrganisationnelService serviceService,
            SalarieService salarieService, UtilisateurService utilisateurService,
            RoleService roleService, RoleRepository roleRepository,
            PermissionService permissionService, PermissionRepository permissionRepository,
            OrganisationImportService organisationImportService) {
        this.permissionEvaluator = permissionEvaluator;
        this.auditService = auditService;
        this.utilisateurRepository = utilisateurRepository;

        // --- Exports ---
        exports.put("plans-strategiques", new Export("plan.exporter", "plans_strategiques",
                (ids, a) -> planService.exporterEnCsv(ordonner(uuids(ids), planRepository.findAllById(uuids(ids)), p -> p.getId())),
                (ids, a) -> planService.exporterEnPdf(ordonner(uuids(ids), planRepository.findAllById(uuids(ids)), p -> p.getId()))));
        exports.put("objectifs-strategiques", new Export("os.exporter", "objectifs_strategiques",
                (ids, a) -> osService.exporterEnCsv(ordonner(uuids(ids), osRepository.findAllById(uuids(ids)), o -> o.getId())),
                (ids, a) -> osService.exporterEnPdf(ordonner(uuids(ids), osRepository.findAllById(uuids(ids)), o -> o.getId()))));
        exports.put("objectifs-operationnels", new Export("op.exporter", "objectifs_operationnels",
                (ids, a) -> opService.exporterEnCsv(ordonner(uuids(ids), opRepository.findAllById(uuids(ids)), o -> o.getId())),
                (ids, a) -> opService.exporterEnPdf(ordonner(uuids(ids), opRepository.findAllById(uuids(ids)), o -> o.getId()))));
        exports.put("activites-catalogue", new Export("activite.exporter", "activites_catalogue",
                (ids, a) -> activiteService.exporterEnCsv(ordonner(uuids(ids), activiteRepository.findAllById(uuids(ids)), o -> o.getId())),
                (ids, a) -> activiteService.exporterEnPdf(ordonner(uuids(ids), activiteRepository.findAllById(uuids(ids)), o -> o.getId()))));
        exports.put("nomenclature", new Export("os.exporter", "nomenclature",
                (ids, a) -> budgetExportService.nomenclatureCsv(uuids(ids)),
                (ids, a) -> budgetExportService.nomenclaturePdf(uuids(ids))));
        exports.put("allocations", new Export("allocation_os.exporter", "allocations",
                (ids, a) -> budgetExportService.allocationsCsv(uuids(ids)),
                (ids, a) -> budgetExportService.allocationsPdf(uuids(ids))));
        exports.put("exercices-budgetaires", new Export("exercice.exporter", "exercices_budgetaires",
                (ids, a) -> exerciceService.exporterEnCsv(ordonner(uuids(ids), exerciceRepository.findAllById(uuids(ids)), e -> e.getId())),
                (ids, a) -> exerciceService.exporterEnPdf(ordonner(uuids(ids), exerciceRepository.findAllById(uuids(ids)), e -> e.getId()))));
        exports.put("allocations-os", new Export("allocation_os.exporter", "allocations_os",
                (ids, a) -> cascadeService.exporterOsCsv(ordonner(uuids(ids), allocationOsRepository.findAllById(uuids(ids)), x -> x.getId())),
                (ids, a) -> cascadeService.exporterOsPdf(ordonner(uuids(ids), allocationOsRepository.findAllById(uuids(ids)), x -> x.getId()))));
        exports.put("allocations-op", new Export("allocation_op.exporter", "allocations_op",
                (ids, a) -> cascadeService.exporterOpCsv(ordonner(uuids(ids), allocationOpRepository.findAllById(uuids(ids)), x -> x.getId())),
                (ids, a) -> cascadeService.exporterOpPdf(ordonner(uuids(ids), allocationOpRepository.findAllById(uuids(ids)), x -> x.getId()))));
        exports.put("allocations-activites", new Export("allocation_activite.exporter", "allocations_activites",
                (ids, a) -> cascadeService.exporterActiviteCsv(ordonner(uuids(ids), allocationActiviteRepository.findAllById(uuids(ids)), x -> x.getId())),
                (ids, a) -> cascadeService.exporterActivitePdf(ordonner(uuids(ids), allocationActiviteRepository.findAllById(uuids(ids)), x -> x.getId()))));
        exports.put("allocations-structures", new Export("execution.exporter", "allocations_structures",
                (ids, a) -> allocationStructureService.exporterEnCsv(ordonner(uuids(ids), allocationStructureRepository.findAllById(uuids(ids)), x -> x.getId())),
                (ids, a) -> allocationStructureService.exporterEnPdf(ordonner(uuids(ids), allocationStructureRepository.findAllById(uuids(ids)), x -> x.getId()))));
        exports.put("statistiques-cles", new Export("statistique_cle.exporter", "statistiques_cles",
                (ids, a) -> statistiqueService.exporterEnCsv(ordonner(uuids(ids), statistiqueRepository.findAllById(uuids(ids)), x -> x.getId())),
                (ids, a) -> statistiqueService.exporterEnPdf(ordonner(uuids(ids), statistiqueRepository.findAllById(uuids(ids)), x -> x.getId()))));
        exports.put("repartition-csp", new Export("repartition_csp.exporter", "repartition_csp",
                (ids, a) -> repartitionService.exporterEnCsv(ordonner(uuids(ids), repartitionRepository.findAllById(uuids(ids)), x -> x.getId())),
                (ids, a) -> repartitionService.exporterEnPdf(ordonner(uuids(ids), repartitionRepository.findAllById(uuids(ids)), x -> x.getId()))));
        exports.put("revisions-budgetaires", new Export("revision.exporter", "revisions_budgetaires",
                (ids, a) -> revisionService.exporterEnCsv(revisionsVisibles(ids, a, demandeRepository)),
                (ids, a) -> revisionService.exporterEnPdf(revisionsVisibles(ids, a, demandeRepository))));
        exports.put("journal-mouvements", new Export("journal.exporter", "journal_mouvements",
                (ids, a) -> journalService.exporterEnCsv(ordonner(uuids(ids), journalRepository.findAllById(uuids(ids)), x -> x.getId())),
                (ids, a) -> journalService.exporterEnPdf(ordonner(uuids(ids), journalRepository.findAllById(uuids(ids)), x -> x.getId()))));
        exports.put("audit-logs", new Export("audit.exporter", "journal_audit",
                (ids, a) -> auditLogService.exporterEnCsv(ordonner(uuids(ids), auditLogRepository.findAllById(uuids(ids)), x -> x.getId())),
                (ids, a) -> auditLogService.exporterEnPdf(ordonner(uuids(ids), auditLogRepository.findAllById(uuids(ids)), x -> x.getId()))));
        exports.put("structures", new Export("entite.exporter", "structures",
                (ids, a) -> structureService.exporterEnCsv(ordonner(ids, structureRepository.findAllById(ids), s -> s.getCode())),
                (ids, a) -> structureService.exporterEnPdf(ordonner(ids, structureRepository.findAllById(ids), s -> s.getCode()))));
        exports.put("directions", new Export("direction.exporter", "directions",
                (ids, a) -> directionService.exporterEnCsv(ordonner(uuids(ids), directionService.lister(null, null, null), DirectionResponseDto::id)),
                (ids, a) -> directionService.exporterEnPdf(ordonner(uuids(ids), directionService.lister(null, null, null), DirectionResponseDto::id))));
        exports.put("services", new Export("service.exporter", "services",
                (ids, a) -> serviceService.exporterEnCsv(ordonner(uuids(ids), serviceService.lister(null, null, null, null), ServiceResponseDto::id)),
                (ids, a) -> serviceService.exporterEnPdf(ordonner(uuids(ids), serviceService.lister(null, null, null, null), ServiceResponseDto::id))));
        exports.put("salaries", new Export("salarie.exporter", "salaries",
                (ids, a) -> salarieService.exporterEnCsv(ordonner(uuids(ids), salarieService.lister(null, null, null), SalarieResponseDto::id)),
                (ids, a) -> salarieService.exporterEnPdf(ordonner(uuids(ids), salarieService.lister(null, null, null), SalarieResponseDto::id))));
        exports.put("utilisateurs", new Export("utilisateur.exporter", "utilisateurs",
                (ids, a) -> utilisateurService.exporterEnCsv(ordonner(uuids(ids), utilisateurService.listerTousLesUtilisateurs(), UtilisateurResponseDto::id)),
                (ids, a) -> utilisateurService.exporterEnPdf(ordonner(uuids(ids), utilisateurService.listerTousLesUtilisateurs(), UtilisateurResponseDto::id))));
        exports.put("roles", new Export("role.exporter", "roles",
                (ids, a) -> roleService.exporterEnCsv(ordonner(uuids(ids), roleRepository.findAllById(uuids(ids)), r -> r.getId())),
                (ids, a) -> roleService.exporterEnPdf(ordonner(uuids(ids), roleRepository.findAllById(uuids(ids)), r -> r.getId()))));
        exports.put("permissions", new Export("permission.exporter", "permissions",
                (ids, a) -> permissionService.exporterEnCsv(ordonner(uuids(ids), permissionRepository.findAllById(uuids(ids)), p -> p.getId())),
                (ids, a) -> permissionService.exporterEnPdf(ordonner(uuids(ids), permissionRepository.findAllById(uuids(ids)), p -> p.getId()))));

        // --- Imports ---
        imports.put("plans-strategiques", new Import(List.of("plan.importer"), (f, a) -> budgetImportService.importerPlans(f)));
        imports.put("nomenclature", new Import(List.of("os.importer", "op.importer", "activite.importer"),
                (f, a) -> budgetImportService.importerNomenclature(f)));
        imports.put("objectifs-strategiques", new Import(List.of("os.importer"),
                (f, a) -> budgetImportService.importerNomenclature(f, "OS")));
        imports.put("objectifs-operationnels", new Import(List.of("op.importer"),
                (f, a) -> budgetImportService.importerNomenclature(f, "OP")));
        imports.put("activites-catalogue", new Import(List.of("activite.importer"),
                (f, a) -> budgetImportService.importerNomenclature(f, "AC")));
        imports.put("exercices-budgetaires", new Import(List.of("exercice.importer"), (f, a) -> budgetImportService.importerExercices(f)));
        imports.put("allocations", new Import(List.of("allocation.importer"), (f, a) -> budgetImportService.importerAllocations(f)));
        imports.put("statistiques-cles", new Import(List.of("statistique_cle.importer"), (f, a) -> budgetImportService.importerStatistiques(f)));
        imports.put("repartition-csp", new Import(List.of("repartition_csp.importer"), (f, a) -> budgetImportService.importerRepartitionsCsp(f)));
        imports.put("revisions-budgetaires", new Import(List.of("revision.importer"),
                (f, a) -> budgetImportService.importerRevisions(f, utilisateurId(a))));
        imports.put("directions", new Import(List.of("direction.importer"), (f, a) -> organisationImportService.importerDirections(f)));
        imports.put("services", new Import(List.of("service.importer"), (f, a) -> organisationImportService.importerServices(f)));
        imports.put("salaries", new Import(List.of("salarie.importer"), (f, a) -> organisationImportService.importerSalaries(f)));
    }

    @PostMapping("/exports/{ressource}/{format}")
    public ResponseEntity<byte[]> exporter(
            @PathVariable String ressource,
            @PathVariable String format,
            @RequestBody SelectionExport selection,
            Authentication authentication) {
        Export export = exports.get(ressource);
        if (export == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Export inconnu : " + ressource);
        }
        exigerPermission(authentication, List.of(export.permission()));
        List<String> ids = selection == null || selection.ids() == null ? List.of()
                : selection.ids().stream().filter(Objects::nonNull).distinct().toList();
        if (ids.size() > MAX_LIGNES_EXPORT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'export est limité à " + MAX_LIGNES_EXPORT + " lignes : affinez vos filtres.");
        }

        byte[] contenu;
        MediaType type;
        if ("csv".equals(format)) {
            byte[] texte = export.csv().apply(ids, authentication).getBytes(StandardCharsets.UTF_8);
            // BOM : accents correctement reconnus à l'ouverture dans Excel.
            contenu = new byte[BOM_UTF8.length + texte.length];
            System.arraycopy(BOM_UTF8, 0, contenu, 0, BOM_UTF8.length);
            System.arraycopy(texte, 0, contenu, BOM_UTF8.length, texte.length);
            type = new MediaType("text", "csv", StandardCharsets.UTF_8);
        } else if ("pdf".equals(format)) {
            contenu = export.pdf().apply(ids, authentication);
            type = MediaType.APPLICATION_PDF;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format d'export inconnu : " + format);
        }

        auditService.logAction(utilisateurId(authentication), "EXPORT", tronquer(ressource), format,
                null, Map.of("format", format, "lignes", ids.size()));
        String nom = export.nomFichier() + "_" + LocalDate.now() + "." + format;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom).build().toString())
                .body(contenu);
    }

    @PostMapping("/imports/{ressource}")
    public ResponseEntity<Map<String, Object>> importer(
            @PathVariable String ressource,
            @RequestParam("file") MultipartFile fichier,
            Authentication authentication) {
        Import definition = imports.get(ressource);
        if (definition == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Import inconnu : " + ressource);
        }
        exigerPermission(authentication, definition.permissions());
        int importes = definition.traitement().apply(fichier, authentication);
        auditService.logAction(utilisateurId(authentication), "IMPORT", tronquer(ressource),
                fichier.getOriginalFilename() == null ? "csv" : tronquer(fichier.getOriginalFilename()),
                null, Map.of("elementsImportes", importes));
        return ResponseEntity.ok(Map.of(
                "message", importes + " élément(s) importé(s) avec succès.",
                "elementsImportes", importes));
    }

    // --- Aides ---

    private void exigerPermission(Authentication authentication, List<String> permissions) {
        boolean autorise = permissions.stream()
                .anyMatch(permission -> permissionEvaluator.hasPermission(authentication, null, permission));
        if (!autorise) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Permission requise : " + String.join(" ou ", permissions) + ".");
        }
    }

    private List<DemandeRevisionBudgetaire> revisionsVisibles(
            List<String> ids, Authentication authentication, DemandeRevisionBudgetaireRepository repository) {
        UUID utilisateurId = utilisateurId(authentication);
        boolean peutValider = permissionEvaluator.hasPermission(authentication, null, "revision.valider");
        List<DemandeRevisionBudgetaire> visibles = repository.findAllById(uuids(ids)).stream()
                .filter(demande -> RevisionBudgetaireService.estVisible(demande, utilisateurId, peutValider))
                .toList();
        return ordonner(uuids(ids), visibles, DemandeRevisionBudgetaire::getId);
    }

    private UUID utilisateurId(Authentication authentication) {
        return utilisateurRepository.findByUsername(authentication.getName())
                .map(Utilisateur::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur authentifié introuvable."));
    }

    private static List<UUID> uuids(List<String> ids) {
        List<UUID> resultat = new ArrayList<>(ids.size());
        for (String id : ids) {
            try {
                resultat.add(UUID.fromString(id));
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant invalide dans la sélection : " + id);
            }
        }
        return resultat;
    }

    /** Remet les éléments dans l'ordre des identifiants fournis (ordre d'affichage) ; ignore les absents. */
    static <K, T> List<T> ordonner(List<K> ids, Collection<T> elements, Function<T, K> cle) {
        Map<K, T> parCle = new HashMap<>();
        for (T element : elements) {
            parCle.put(cle.apply(element), element);
        }
        return ids.stream().map(parCle::get).filter(Objects::nonNull).toList();
    }

    private static String tronquer(String valeur) {
        return valeur.length() > 50 ? valeur.substring(0, 50) : valeur;
    }
}
