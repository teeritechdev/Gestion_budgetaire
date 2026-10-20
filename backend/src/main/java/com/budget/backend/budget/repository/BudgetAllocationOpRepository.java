package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.BudgetAllocationOp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetAllocationOpRepository extends JpaRepository<BudgetAllocationOp, UUID>, JpaSpecificationExecutor<BudgetAllocationOp> {
    List<BudgetAllocationOp> findByBudgetAllocationOsId(UUID allocationOsId);
    List<BudgetAllocationOp> findByObjectifOperationnelId(UUID opId);
}