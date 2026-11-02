package com.budget.backend.budget.service;

import com.budget.backend.budget.dto.ActiviteCatalogueRequestDto;
import com.budget.backend.budget.dto.EntiteStatistiqueCleRequestDto;
import com.budget.backend.budget.dto.ExerciceBudgetaireRequestDto;
import com.budget.backend.budget.dto.LigneRevisionRequestDto;
import com.budget.backend.budget.dto.ObjectifOperationnelRequestDto;
import com.budget.backend.budget.dto.ObjectifStrategiqueRequestDto;
import com.budget.backend.budget.dto.PlanStrategiqueRequestDto;
import com.budget.backend.budget.dto.RepartitionCspRuleRequestDto;
import com.budget.backend.budget.entity.ActiviteCatalogue;
import com.budget.backend.budget.entity.BudgetAllocationActivite;
import com.budget.backend.budget.entity.BudgetAllocationOp;
import com.budget.backend.budget.entity.BudgetAllocationOs;
import com.budget.backend.budget.entity.DemandeRevisionBudgetaire;
import com.budget.backend.budget.entity.ExerciceBudgetaire;
import com.budget.backend.budget.entity.ObjectifOperationnel;
import com.budget.backend.budget.entity.ObjectifStrategique;
import com.budget.backend.budget.entity.PlanStrategique;
import com.budget.backend.budget.entity.RepartitionCspCleType;
import com.budget.backend.budget.repository.ActiviteCatalogueRepository;
import com.budget.backend.budget.repository.BudgetAllocationActiviteRepository;
import com.budget.backend.budget.repository.BudgetAllocationOpRepository;
import com.budget.backend.budget.repository.BudgetAllocationOsRepository;
import com.budget.backend.budget.repository.ExerciceBudgetaireRepository;
import com.budget.backend.budget.repository.ObjectifOperationnelRepository;
import com.budget.backend.budget.repository.ObjectifStrategiqueRepository;
import com.budget.backend.budget.repository.PlanStrategiqueRepository;
import com.budget.backend.common.CsvReader;
import com.budget.backend.common.CsvReader.Ligne;
import com.budget.backend.security.security.CustomPermissionEvaluator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Imports CSV du module budget. Chaque import est transactionnel (tout ou rien) et passe par
 * les mêmes validations et services métier que la saisie à l'écran.
 */
@Service
public class BudgetImportService {

    private final Validator validator;
    private final CustomPermissionEvaluator permissionEvaluator;
    private final PlanStrategiqueService planService;
    private final ObjectifStrategiqueService osService;
    private final ObjectifOperationnelService opService;
    private final ActiviteCatalogueService activiteService;
    private final ExerciceBudgetaireService exerciceService;
    private final EntiteStatistiqueCleService statistiqueService;
    private final RepartitionCspRuleService repartitionService;
    private final RevisionBudgetaireService revisionService;
    private final CascadeAllocationService cascadeService;
    private final BudgetAllocationStructureService allocationStructureService;
    private final PlanStrategiqueRepository planRepository;
    private final ObjectifStrategiqueRepository osRepository;
    private final ObjectifOperationnelRepository opRepository;
    private final ActiviteCatalogueRepository activiteRepository;
    private final ExerciceBudgetaireRepository exerciceRepository;
    private final BudgetAllocationOsRepository allocationOsRepository;
    private final BudgetAllocationOpRepository allocationOpRepository;
    private final BudgetAllocationActiviteRepository allocationActiviteRepository;

    public BudgetImportService(
            Validator validator,
            CustomPermissionEvaluator permissionEvaluator,
            PlanStrategiqueService planService,
            ObjectifStrategiqueService osService,
            ObjectifOperationnelService opService,
            ActiviteCatalogueService activiteService,
            ExerciceBudgetaireService exerciceService,
            EntiteStatistiqueCleService statistiqueService,
            RepartitionCspRuleService repartitionService,
            RevisionBudgetaireService revisionService,
            CascadeAllocationService cascadeService,
            BudgetAllocationStructureService allocationStructureService,
            PlanStrategiqueRepository planRepository,
            ObjectifStrategiqueRepository osRepository,
            ObjectifOperationnelRepository opRepository,
            ActiviteCatalogueRepository activiteRepository,
            ExerciceBudgetaireRepository exerciceRepository,
            BudgetAllocationOsRepository allocationOsRepository,
            BudgetAllocationOpRepository allocationOpRepository,
            BudgetAllocationActiviteRepository allocationActiviteRepository) {
        this.validator = validator;
        this.permissionEvaluator = permissionEvaluator;
        this.planService = planService;
        this.osService = osService;
        this.opService = opService;
        this.activiteService = activiteService;
        this.exerciceService = exerciceService;
        this.statistiqueService = statistiqueService;
        this.repartitionService = repartitionService;
        this.revisionService = revisionService;
        this.cascadeService = cascadeService;
        this.allocationStructureService = allocationStructureService;
        this.planRepository = planRepository;
        this.osRepository = osRepository;
        this.opRepository = opRepository;
        this.activiteRepository = activiteRepository;
        this.exerciceRepository = exerciceRepository;
        this.allocationOsRepository = allocationOsRepository;
        this.allocationOpRepository = allocationOpRepository;
        this.allocationActiviteRepository = allocationActiviteRepository;
    }

    // --- Plans budgétaires : code_plan;libelle;date_debut;date_fin;actif ---
    @Transactional
    public int importerPlans(MultipartFile fichier) {
        return CsvReader.lire(fichier, List.of("code_plan", "libelle", "date_debut", "date_fin"), ligne -> {
            PlanStrategiqueRequestDto dto = new PlanStrategiqueRequestDto();
            dto.setCodePlan(ligne.texte("code_plan"));
            dto.setLibelle(ligne.texte("libelle"));
            dto.setDateDebut(ligne.date("date_debut"));
            dto.setDateFin(ligne.date("date_fin"));
            dto.setActif(ligne.booleen(true, "actif"));
            valider(dto);
            planService.creer(PlanStrategique.builder()
                    .codePlan(dto.getCodePlan().trim())
                    .libelle(dto.getLibelle().trim())
                    .dateDebut(dto.getDateDebut())
                    .dateFin(dto.getDateFin())
                    .actif(dto.getActif())
                    .build());
        });
    }

    /**
     * Nomenclature : soit une colonne « type » (OS, OP ou AC) avec code;libelle;description;code_parent,
     * soit les fichiers exportés par niveau (code_os / code_op / code_activite). Les lignes sont traitées
     * dans l'ordre : un parent peut être créé plus haut dans le même fichier.
     */
    @Transactional
    public int importerNomenclature(MultipartFile fichier) {
        return importerNomenclature(fichier, null);
    }

    /**
     * Import d'un seul niveau (sous-onglet OS, OP ou AC) : colonnes de l'export correspondant
     * (code_os / code_op / code_activite, libelle, description, code du parent).
     */
    @Transactional
    public int importerNomenclature(MultipartFile fichier, String typeImpose) {
        List<String> requises = typeImpose == null ? List.of("libelle") : switch (typeImpose) {
            case "OS" -> List.of("code_os|code", "libelle");
            case "OP" -> List.of("code_op|code", "libelle", "code_os|code_parent");
            default -> List.of("code_activite|code", "libelle", "code_op|code_parent");
        };
        return CsvReader.lire(fichier, requises, ligne -> {
            String type = typeImpose != null ? typeImpose : typeNomenclature(ligne);
            String typeLigne = ligne.texte("type", "niveau");
            if (typeImpose != null && typeLigne != null && !typeImpose.equalsIgnoreCase(typeLigne.trim())) {
                throw new IllegalArgumentException("cette ligne est de type « " + typeLigne
                        + " » : importez-la depuis le sous-onglet correspondant");
            }
            exigerPermission(Map.of("OS", "os.importer", "OP", "op.importer", "AC", "activite.importer").get(type));
            String code = ligne.texte("code", "code_" + Map.of("OS", "os", "OP", "op", "AC", "activite").get(type));
            String libelle = ligne.texte("libelle");
            String description = ligne.texte("description");
            switch (type) {
                case "OS" -> {
                    ObjectifStrategiqueRequestDto dto = new ObjectifStrategiqueRequestDto();
                    dto.setCodeOs(code);
                    dto.setLibelle(libelle);
                    dto.setDescription(description);
                    valider(dto);
                    osService.creer(ObjectifStrategique.builder()
                            .codeOs(code.trim()).libelle(libelle.trim()).description(description).build());
                }
                case "OP" -> {
                    String codeOs = ligne.texte("code_parent", "code_os");
                    ObjectifStrategique parent = osRepository.findByCodeOs(exiger(codeOs, "code_parent (code de l'OS)"))
                            .orElseThrow(() -> new IllegalArgumentException("objectif stratégique « " + codeOs + " » introuvable"));
                    ObjectifOperationnelRequestDto dto = new ObjectifOperationnelRequestDto();
                    dto.setOsId(parent.getId());
                    dto.setCodeOp(code);
                    dto.setLibelle(libelle);
                    dto.setDescription(description);
                    valider(dto);
                    opService.creer(parent.getId(), ObjectifOperationnel.builder()
                            .codeOp(code.trim()).libelle(libelle.trim()).description(description).build());
                }
                default -> {
                    String codeOp = ligne.texte("code_parent", "code_op");
                    ObjectifOperationnel parent = opRepository.findByCodeOp(exiger(codeOp, "code_parent (code de l'OP)"))
                            .orElseThrow(() -> new IllegalArgumentException("objectif opérationnel « " + codeOp + " » introuvable"));
                    ActiviteCatalogueRequestDto dto = new ActiviteCatalogueRequestDto();
                    dto.setOpId(parent.getId());
                    dto.setCodeActivite(code);
                    dto.setLibelle(libelle);
                    dto.setDescription(description);
                    valider(dto);
                    activiteService.creer(parent.getId(), ActiviteCatalogue.builder()
                            .codeActivite(code.trim()).libelle(libelle.trim()).description(description).build());
                }
            }
        });
    }

    // --- Exercices : annee;code_plan;statut;montant_total_vote;date_ouverture;date_fermeture ---
    @Transactional
    public int importerExercices(MultipartFile fichier) {
        return CsvReader.lire(fichier, List.of("annee", "code_plan", "montant_total_vote"), ligne -> {
            String codePlan = ligne.obligatoire("code_plan");
            PlanStrategique plan = planRepository.findByCodePlan(codePlan)
                    .orElseThrow(() -> new IllegalArgumentException("plan budgétaire « " + codePlan + " » introuvable"));
            ExerciceBudgetaireRequestDto dto = new ExerciceBudgetaireRequestDto();
            dto.setAnnee(ligne.entier("annee"));
            dto.setPlanId(plan.getId());
            String statut = ligne.texte("statut");
            dto.setStatut(statut == null ? "PREPARATION" : statut.trim().toUpperCase(Locale.ROOT));
            dto.setMontantTotalVote(ligne.decimal("montant_total_vote"));
            dto.setDateOuverture(ligne.date("date_ouverture"));
            dto.setDateFermeture(ligne.date("date_fermeture"));
            valider(dto);
            exerciceService.creer(plan.getId(), ExerciceBudgetaire.builder()
                    .annee(dto.getAnnee())
                    .statut(dto.getStatut())
                    .montantTotalVote(dto.getMontantTotalVote())
                    .dateOuverture(dto.getDateOuverture())
                    .dateFermeture(dto.getDateFermeture())
                    .build());
        });
    }

    /**
     * Allocations de la cascade : niveau;annee;code_os;code_op;code_activite;code_structure;montant.
     * Niveau OS, OP, AC ou STRUCTURE ; chaque niveau s'appuie sur l'allocation parente (éventuellement
     * créée plus haut dans le fichier). Les enveloppes sont contrôlées comme à l'écran.
     */
    @Transactional
    public int importerAllocations(MultipartFile fichier) {
        return CsvReader.lire(fichier, List.of("niveau", "annee", "code_os", "montant"), ligne -> {
            String niveau = ligne.obligatoire("niveau").trim().toUpperCase(Locale.ROOT);
            Integer annee = ligne.entier("annee");
            BigDecimal montant = ligne.decimal("montant");
            ExerciceBudgetaire exercice = exerciceRepository.findByAnnee(exiger(annee, "annee"))
                    .orElseThrow(() -> new IllegalArgumentException("aucun exercice budgétaire pour l'année " + annee));
            String codeOs = ligne.obligatoire("code_os");
            switch (niveau) {
                case "OS" -> {
                    exigerPermission("allocation_os.creer");
                    ObjectifStrategique os = osRepository.findByCodeOs(codeOs)
                            .orElseThrow(() -> new IllegalArgumentException("objectif stratégique « " + codeOs + " » introuvable"));
                    cascadeService.allouerExerciceVersOs(exercice.getId(), os.getId(), montant);
                }
                case "OP" -> {
                    exigerPermission("allocation_op.creer");
                    BudgetAllocationOs parent = allocationOs(exercice, codeOs);
                    String codeOp = ligne.obligatoire("code_op");
                    ObjectifOperationnel op = opRepository.findByCodeOp(codeOp)
                            .orElseThrow(() -> new IllegalArgumentException("objectif opérationnel « " + codeOp + " » introuvable"));
                    cascadeService.allouerOsVersOp(parent.getId(), op.getId(), montant);
                }
                case "AC" -> {
                    exigerPermission("allocation_activite.creer");
                    BudgetAllocationOp parent = allocationOp(allocationOs(exercice, codeOs), ligne.obligatoire("code_op"));
                    String codeActivite = ligne.obligatoire("code_activite");
                    ActiviteCatalogue activite = activiteRepository.findByCodeActivite(codeActivite)
                            .orElseThrow(() -> new IllegalArgumentException("activité « " + codeActivite + " » introuvable"));
                    cascadeService.allouerOpVersActivite(parent.getId(), activite.getId(), montant);
                }
                case "STRUCTURE" -> {
                    exigerPermission("execution.creer");
                    BudgetAllocationOp op = allocationOp(allocationOs(exercice, codeOs), ligne.obligatoire("code_op"));
                    BudgetAllocationActivite parent = allocationActivite(op, ligne.obligatoire("code_activite"));
                    allocationStructureService.attribuerBudgetStructure(
                            parent.getId(), ligne.obligatoire("code_structure"), montant);
                }
                default -> throw new IllegalArgumentException(
                        "niveau « " + niveau + " » inconnu : utilisez OS, OP, AC ou STRUCTURE");
            }
        });
    }

    // --- Clés statistiques : code_entite;annee;mois;effectif_agents;chiffre_affaires ---
    @Transactional
    public int importerStatistiques(MultipartFile fichier) {
        return CsvReader.lire(fichier, List.of("code_entite|entite_code", "annee|annee_exercice"), ligne -> {
            EntiteStatistiqueCleRequestDto dto = new EntiteStatistiqueCleRequestDto();
            dto.setEntiteCode(ligne.texte("code_entite", "entite_code"));
            dto.setAnneeExercice(ligne.entier("annee", "annee_exercice"));
            dto.setMois(ligne.entier("mois"));
            dto.setEffectifAgents(ligne.entier("effectif_agents", "effectif"));
            dto.setChiffreAffaires(ligne.decimal("chiffre_affaires", "chiffre_d_affaires"));
            valider(dto);
            statistiqueService.creer(dto);
        });
    }

    /**
     * Répartition CSP (format de l'export) : allocation_structure_csp;id_activite_csp;entite_beneficiaire;
     * id_activite_beneficiaire;type_cle;ratio. Les lignes d'une même allocation/activité/clé forment une
     * répartition dont le total doit être égal à 1 (100 %).
     */
    @Transactional
    public int importerRepartitionsCsp(MultipartFile fichier) {
        Map<String, List<RepartitionCspRuleRequestDto>> groupes = new LinkedHashMap<>();
        Map<String, String> lignesParGroupe = new LinkedHashMap<>();
        int lues = CsvReader.lire(fichier, List.of("allocation_structure_csp|allocation_structure_id",
                "id_activite_csp|activite_csp_id", "entite_beneficiaire", "id_activite_beneficiaire|activite_beneficiaire_id",
                "type_cle|cle_type", "ratio_0_a_1|ratio|valeur_cle"), ligne -> {
            RepartitionCspRuleRequestDto dto = new RepartitionCspRuleRequestDto();
            dto.setAllocationStructureId(ligne.uuid("allocation_structure_csp", "allocation_structure_id"));
            dto.setActiviteCspId(ligne.uuid("id_activite_csp", "activite_csp_id"));
            dto.setEntiteBeneficiaire(ligne.obligatoire("entite_beneficiaire"));
            dto.setActiviteBeneficiaireId(ligne.uuid("id_activite_beneficiaire", "activite_beneficiaire_id"));
            String cle = ligne.obligatoire("type_cle", "cle_type").trim().toUpperCase(Locale.ROOT);
            try {
                dto.setCleType(RepartitionCspCleType.valueOf(cle));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("type de clé « " + cle + " » inconnu");
            }
            dto.setValeurCle(ligne.decimal("ratio_0_a_1", "ratio", "valeur_cle"));
            String cleGroupe = dto.getAllocationStructureId() + "|" + dto.getActiviteCspId() + "|"
                    + dto.getActiviteBeneficiaireId() + "|" + dto.getCleType();
            groupes.computeIfAbsent(cleGroupe, k -> new ArrayList<>()).add(dto);
            lignesParGroupe.merge(cleGroupe, String.valueOf(ligne.numero()), (a, b) -> a + ", " + b);
        });
        groupes.forEach((cle, dtos) -> {
            try {
                repartitionService.enregistrerRepartition(dtos);
            } catch (ResponseStatusException exception) {
                throw new ResponseStatusException(exception.getStatusCode(),
                        "Lignes " + lignesParGroupe.get(cle) + " : " + exception.getReason());
            }
        });
        return lues;
    }

    /**
     * Demandes de révision (créées au statut brouillon, au nom de l'utilisateur connecté) :
     * numero_demande;annee;motif;type_mouvement;montant;id_allocation_source;id_allocation_destination.
     * Les lignes portant le même numéro forment une seule demande.
     */
    @Transactional
    public int importerRevisions(MultipartFile fichier, UUID demandeurId) {
        record Demande(Integer annee, String motif, List<LigneRevisionRequestDto> lignes, List<Integer> numeros) {}
        Map<String, Demande> demandes = new LinkedHashMap<>();
        CsvReader.lire(fichier, List.of("numero_demande", "annee|annee_exercice", "motif", "type_mouvement", "montant",
                "id_allocation_destination|allocation_destination_id"), ligne -> {
            String numero = ligne.obligatoire("numero_demande");
            Demande demande = demandes.computeIfAbsent(numero, n -> new Demande(
                    ligne.entier("annee", "annee_exercice"), ligne.texte("motif"), new ArrayList<>(), new ArrayList<>()));
            LigneRevisionRequestDto dto = new LigneRevisionRequestDto();
            dto.setTypeMouvement(ligne.obligatoire("type_mouvement").trim().toUpperCase(Locale.ROOT));
            dto.setMontant(ligne.decimal("montant"));
            dto.setAllocationSourceId(ligne.uuid("id_allocation_source", "allocation_source_id"));
            dto.setAllocationDestinationId(ligne.uuid("id_allocation_destination", "allocation_destination_id"));
            demande.lignes().add(dto);
            demande.numeros().add(ligne.numero());
        });
        demandes.forEach((numero, demande) -> {
            try {
                revisionService.creerDemandeAvecLignes(DemandeRevisionBudgetaire.builder()
                        .numeroDemande(numero)
                        .anneeExercice(demande.annee())
                        .motif(demande.motif())
                        .demandeurId(demandeurId)
                        .build(), demande.lignes());
            } catch (ResponseStatusException exception) {
                throw new ResponseStatusException(exception.getStatusCode(),
                        "Demande " + numero + " (lignes " + demande.numeros() + ") : " + exception.getReason());
            }
        });
        return demandes.size();
    }

    // --- Aides ---

    private String typeNomenclature(Ligne ligne) {
        String type = ligne.texte("type", "niveau");
        if (type != null) {
            String t = type.trim().toUpperCase(Locale.ROOT);
            if (Set.of("OS", "OP", "AC").contains(t)) {
                return t;
            }
            throw new IllegalArgumentException("type « " + type + " » inconnu : utilisez OS, OP ou AC");
        }
        if (ligne.aColonne("code_activite")) {
            return "AC";
        }
        if (ligne.aColonne("code_op")) {
            return "OP";
        }
        if (ligne.aColonne("code_os")) {
            return "OS";
        }
        throw new IllegalArgumentException("ajoutez une colonne « type » (OS, OP ou AC)");
    }

    private BudgetAllocationOs allocationOs(ExerciceBudgetaire exercice, String codeOs) {
        List<BudgetAllocationOs> trouvees = allocationOsRepository.findByExerciceBudgetaireId(exercice.getId()).stream()
                .filter(a -> a.getObjectifStrategique() != null && codeOs.equals(a.getObjectifStrategique().getCodeOs()))
                .toList();
        return unique(trouvees, "allocation de l'OS « " + codeOs + " » sur l'exercice " + exercice.getAnnee());
    }

    private BudgetAllocationOp allocationOp(BudgetAllocationOs parent, String codeOp) {
        List<BudgetAllocationOp> trouvees = allocationOpRepository.findByBudgetAllocationOsId(parent.getId()).stream()
                .filter(a -> a.getObjectifOperationnel() != null && codeOp.equals(a.getObjectifOperationnel().getCodeOp()))
                .toList();
        return unique(trouvees, "allocation de l'OP « " + codeOp + " »");
    }

    private BudgetAllocationActivite allocationActivite(BudgetAllocationOp parent, String codeActivite) {
        List<BudgetAllocationActivite> trouvees = allocationActiviteRepository.findByBudgetAllocationOpId(parent.getId()).stream()
                .filter(a -> a.getActiviteCatalogue() != null && codeActivite.equals(a.getActiviteCatalogue().getCodeActivite()))
                .toList();
        return unique(trouvees, "allocation de l'activité « " + codeActivite + " »");
    }

    private static <T> T unique(List<T> elements, String libelle) {
        if (elements.isEmpty()) {
            throw new IllegalArgumentException(libelle + " introuvable : importez d'abord le niveau parent");
        }
        if (elements.size() > 1) {
            throw new IllegalArgumentException(libelle + " ambiguë : plusieurs allocations correspondent");
        }
        return elements.get(0);
    }

    private static <T> T exiger(T valeur, String colonne) {
        if (valeur == null || (valeur instanceof String texte && texte.isBlank())) {
            throw new IllegalArgumentException("la colonne « " + colonne + " » est obligatoire");
        }
        return valeur;
    }

    private void exigerPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!permissionEvaluator.hasPermission(authentication, null, permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La permission « " + permission + " » est nécessaire pour importer cette ligne.");
        }
    }

    private void valider(Object dto) {
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.iterator().next().getMessage());
        }
    }
}
