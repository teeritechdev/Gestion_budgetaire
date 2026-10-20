package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.BudgetAllocationStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetAllocationStructureRepository extends JpaRepository<BudgetAllocationStructure, UUID>, JpaSpecificationExecutor<BudgetAllocationStructure> {
    List<BudgetAllocationStructure> findByStructureId(UUID structureId);
    List<BudgetAllocationStructure> findByBudgetAllocationActiviteId(UUID allocationActiviteId);
}