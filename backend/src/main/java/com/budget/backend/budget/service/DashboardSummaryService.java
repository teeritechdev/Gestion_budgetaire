package com.budget.backend.budget.service;

import com.budget.backend.budget.dto.DashboardSummaryDto;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.budget.repository.DemandeRevisionBudgetaireRepository;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.AuditLogRepository;
import com.budget.backend.security.repository.DirectionRepository;
import com.budget.backend.security.repository.StructureRepository;
import com.budget.backend.security.repository.PermissionRepository;
import com.budget.backend.security.repository.RoleRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardSummaryService {
    private final UtilisateurRepository utilisateurRepository;
    private final StructureRepository entiteRepository;
    private final DirectionRepository directionRepository;
    private final BudgetAllocationStructureRepository allocationRepository;
    private final DemandeRevisionBudgetaireRepository revisionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardSummaryService(
            UtilisateurRepository utilisateurRepository,
            StructureRepository entiteRepository,
            DirectionRepository directionRepository,
            BudgetAllocationStructureRepository allocationRepository,
            DemandeRevisionBudgetaireRepository revisionRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            AuditLogRepository auditLogRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.entiteRepository = entiteRepository;
        this.directionRepository = directionRepository;
        this.allocationRepository = allocationRepository;
        this.revisionRepository = revisionRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(String username) {
        Utilisateur user = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        List<Utilisateur> scopedUsers = utilisateurRepository.findAll();
        List<Structure> structures = entiteRepository.findAll();
        List<String> structureCodes = structures.stream().map(Structure::getCode).toList();
        List<BudgetAllocationStructure> allocations = structureCodes.isEmpty()
                ? List.of()
                : allocationRepository.findByStructure_CodeIn(structureCodes);

        BigDecimal budgetAlloue = allocations.stream()
                .map(this::allocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal engaged = allocations.stream()
                .map(allocation -> zeroIfNull(allocation.getMontantEngage()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal liquidated = allocations.stream()
                .map(allocation -> zeroIfNull(allocation.getMontantLiquide()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingRevisions = revisionRepository.countByStatut("EN_ATTENTE");
        long roleCount = roleRepository.count();
        long permissionCount = permissionRepository.count();
        long auditCount = auditLogRepository.count();

        return new DashboardSummaryDto(
                true,
                "Toutes les structures",
                "Toutes les entités",
                directionRepository.count(),
                structures.size(),
                scopedUsers.size(),
                scopedUsers.stream().filter(item -> Boolean.TRUE.equals(item.getActif())).count(),
                budgetAlloue,
                engaged,
                liquidated,
                pendingRevisions,
                roleCount,
                permissionCount,
                auditCount
        );
    }

    private BigDecimal allocatedAmount(BudgetAllocationStructure allocation) {
        if (allocation.getMontantAlloue() != null) {
            return allocation.getMontantAlloue();
        }
        return zeroIfNull(allocation.getMontantVote()).add(zeroIfNull(allocation.getMontantModifie()));
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
