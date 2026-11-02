package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.ActiviteCatalogue;
import com.budget.backend.budget.entity.BudgetAllocationActivite;
import com.budget.backend.budget.entity.BudgetAllocationOp;
import com.budget.backend.budget.entity.BudgetAllocationOs;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.entity.ExerciceBudgetaire;
import com.budget.backend.budget.entity.ObjectifOperationnel;
import com.budget.backend.budget.entity.ObjectifStrategique;
import com.budget.backend.budget.repository.ActiviteCatalogueRepository;
import com.budget.backend.budget.repository.BudgetAllocationActiviteRepository;
import com.budget.backend.budget.repository.BudgetAllocationOpRepository;
import com.budget.backend.budget.repository.BudgetAllocationOsRepository;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.budget.repository.ObjectifOperationnelRepository;
import com.budget.backend.budget.repository.ObjectifStrategiqueRepository;
import com.budget.backend.common.CsvUtils;
import com.budget.backend.common.PdfUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exports combinés multi-niveaux, au format exact des imports correspondants (aller-retour possible) :
 * nomenclature (OS, OP, AC) et cascade des allocations (OS, OP, AC, structures).
 */
@Service
@Transactional(readOnly = true)
public class BudgetExportService {

    private static final List<String> ENTETE_NOMENCLATURE = List.of("type", "code", "libelle", "description", "code_parent");
    private static final List<String> ENTETE_ALLOCATIONS = List.of("niveau", "annee", "code_os", "code_op",
            "code_activite", "code_structure", "montant", "montant_modifie", "montant_engage", "montant_liquide",
            "montant_disponible");

    private final ObjectifStrategiqueRepository osRepository;
    private final ObjectifOperationnelRepository opRepository;
    private final ActiviteCatalogueRepository activiteRepository;
    private final BudgetAllocationOsRepository allocationOsRepository;
    private final BudgetAllocationOpRepository allocationOpRepository;
    private final BudgetAllocationActiviteRepository allocationActiviteRepository;
    private final BudgetAllocationStructureRepository allocationStructureRepository;

    public BudgetExportService(
            ObjectifStrategiqueRepository osRepository,
            ObjectifOperationnelRepository opRepository,
            ActiviteCatalogueRepository activiteRepository,
            BudgetAllocationOsRepository allocationOsRepository,
            BudgetAllocationOpRepository allocationOpRepository,
            BudgetAllocationActiviteRepository allocationActiviteRepository,
            BudgetAllocationStructureRepository allocationStructureRepository) {
        this.osRepository = osRepository;
        this.opRepository = opRepository;
        this.activiteRepository = activiteRepository;
        this.allocationOsRepository = allocationOsRepository;
        this.allocationOpRepository = allocationOpRepository;
        this.allocationActiviteRepository = allocationActiviteRepository;
        this.allocationStructureRepository = allocationStructureRepository;
    }

    // --- Nomenclature ---

    public String nomenclatureCsv(List<UUID> ids) {
        StringBuilder csv = new StringBuilder(CsvUtils.ligne(';', ENTETE_NOMENCLATURE.toArray()));
        lignesNomenclature(ids).forEach(ligne -> csv.append(CsvUtils.ligne(';', ligne.toArray())));
        return csv.toString();
    }

    public byte[] nomenclaturePdf(List<UUID> ids) {
        return PdfUtils.tableau("Nomenclature budgétaire (OS · OP · AC)",
                List.of("Type", "Code", "Libellé", "Description", "Parent"), lignesNomenclature(ids));
    }

    private List<List<Object>> lignesNomenclature(List<UUID> ids) {
        Map<UUID, List<Object>> parId = new HashMap<>();
        for (ObjectifStrategique os : osRepository.findAllById(ids)) {
            parId.put(os.getId(), Arrays.asList("OS", os.getCodeOs(), os.getLibelle(), os.getDescription(), null));
        }
        for (ObjectifOperationnel op : opRepository.findAllById(ids)) {
            parId.put(op.getId(), Arrays.asList("OP", op.getCodeOp(), op.getLibelle(), op.getDescription(),
                    op.getObjectifStrategique() == null ? null : op.getObjectifStrategique().getCodeOs()));
        }
        for (ActiviteCatalogue ac : activiteRepository.findAllById(ids)) {
            parId.put(ac.getId(), Arrays.asList("AC", ac.getCodeActivite(), ac.getLibelle(), ac.getDescription(),
                    ac.getObjectifOperationnel() == null ? null : ac.getObjectifOperationnel().getCodeOp()));
        }
        return dansLOrdre(ids, parId);
    }

    // --- Cascade des allocations ---

    public String allocationsCsv(List<UUID> ids) {
        StringBuilder csv = new StringBuilder(CsvUtils.ligne(';', ENTETE_ALLOCATIONS.toArray()));
        lignesAllocations(ids).forEach(ligne -> csv.append(CsvUtils.ligne(';', ligne.toArray())));
        return csv.toString();
    }

    public byte[] allocationsPdf(List<UUID> ids) {
        List<List<Object>> lignes = lignesAllocations(ids).stream()
                .map(l -> Arrays.<Object>asList(l.get(0), l.get(1), l.get(2), l.get(3), l.get(4), l.get(5), l.get(6), l.get(10)))
                .toList();
        return PdfUtils.tableau("Cascade des allocations budgétaires",
                List.of("Niveau", "Exercice", "OS", "OP", "Activité", "Structure", "Montant", "Disponible"), lignes);
    }

    private List<List<Object>> lignesAllocations(List<UUID> ids) {
        Map<UUID, List<Object>> parId = new HashMap<>();
        for (BudgetAllocationOs a : allocationOsRepository.findAllById(ids)) {
            parId.put(a.getId(), ligneAllocation("OS", a, null, null, null, a.getMontantAlloue()));
        }
        for (BudgetAllocationOp a : allocationOpRepository.findAllById(ids)) {
            parId.put(a.getId(), ligneAllocation("OP", a.getBudgetAllocationOs(), a, null, null, a.getMontantAlloue()));
        }
        for (BudgetAllocationActivite a : allocationActiviteRepository.findAllById(ids)) {
            BudgetAllocationOp op = a.getBudgetAllocationOp();
            parId.put(a.getId(), ligneAllocation("AC", op == null ? null : op.getBudgetAllocationOs(), op, a, null,
                    a.getMontantAlloue()));
        }
        for (BudgetAllocationStructure s : allocationStructureRepository.findAllById(ids)) {
            BudgetAllocationActivite ac = s.getBudgetAllocationActivite();
            BudgetAllocationOp op = ac == null ? null : ac.getBudgetAllocationOp();
            List<Object> ligne = ligneAllocation("STRUCTURE", op == null ? null : op.getBudgetAllocationOs(), op, ac,
                    s.getStructure() == null ? null : s.getStructure().getCode(), s.getMontantVote());
            ligne.set(7, s.getMontantModifie());
            ligne.set(8, s.getMontantEngage());
            ligne.set(9, s.getMontantLiquide());
            ligne.set(10, s.getMontantDisponible());
            parId.put(s.getId(), ligne);
        }
        return dansLOrdre(ids, parId);
    }

    private static List<Object> ligneAllocation(String niveau, BudgetAllocationOs os, BudgetAllocationOp op,
                                                BudgetAllocationActivite ac, String codeStructure, Object montant) {
        ExerciceBudgetaire exercice = os == null ? null : os.getExerciceBudgetaire();
        List<Object> ligne = new ArrayList<>(Arrays.asList(new Object[11]));
        ligne.set(0, niveau);
        ligne.set(1, exercice == null ? null : exercice.getAnnee());
        ligne.set(2, os == null || os.getObjectifStrategique() == null ? null : os.getObjectifStrategique().getCodeOs());
        ligne.set(3, op == null || op.getObjectifOperationnel() == null ? null : op.getObjectifOperationnel().getCodeOp());
        ligne.set(4, ac == null || ac.getActiviteCatalogue() == null ? null : ac.getActiviteCatalogue().getCodeActivite());
        ligne.set(5, codeStructure);
        ligne.set(6, montant);
        return ligne;
    }

    private static List<List<Object>> dansLOrdre(List<UUID> ids, Map<UUID, List<Object>> parId) {
        return ids.stream().map(parId::get).filter(java.util.Objects::nonNull).toList();
    }
}
