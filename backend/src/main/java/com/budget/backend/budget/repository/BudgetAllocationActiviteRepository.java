package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.BudgetAllocationActivite;
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
public interface BudgetAllocationActiviteRepository extends JpaRepository<BudgetAllocationActivite, UUID>, JpaSpecificationExecutor<BudgetAllocationActivite> {
    List<BudgetAllocationActivite> findByBudgetAllocationOpId(UUID allocationOpId);
    List<BudgetAllocationActivite> findByActiviteCatalogueId(UUID activiteCatalogueId);

    // Verrou pessimiste : sérialise les allocations concurrentes sur la même enveloppe parente.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from BudgetAllocationActivite e where e.id = :id")
    Optional<BudgetAllocationActivite> findByIdForUpdate(@Param("id") UUID id);
}
