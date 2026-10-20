package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.BudgetAllocationActivite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetAllocationActiviteRepository extends JpaRepository<BudgetAllocationActivite, UUID>, JpaSpecificationExecutor<BudgetAllocationActivite> {
    List<BudgetAllocationActivite> findByBudgetAllocationOpId(UUID allocationOpId);
    List<BudgetAllocationActivite> findByActiviteCatalogueId(UUID activiteCatalogueId);
}