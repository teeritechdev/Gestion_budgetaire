package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.BudgetAllocationOp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

@Repository
public interface BudgetAllocationOpRepository extends JpaRepository<BudgetAllocationOp, UUID>, JpaSpecificationExecutor<BudgetAllocationOp> {
    List<BudgetAllocationOp> findByBudgetAllocationOsId(UUID allocationOsId);
    List<BudgetAllocationOp> findByObjectifOperationnelId(UUID opId);

    // Verrou pessimiste : sérialise les allocations concurrentes sur la même enveloppe parente.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from BudgetAllocationOp e where e.id = :id")
    Optional<BudgetAllocationOp> findByIdForUpdate(@Param("id") UUID id);
}
