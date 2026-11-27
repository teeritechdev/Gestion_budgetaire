package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.BudgetAllocationStructure;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetAllocationStructureRepository extends JpaRepository<BudgetAllocationStructure, UUID>, JpaSpecificationExecutor<BudgetAllocationStructure> {
    List<BudgetAllocationStructure> findByStructure_Code(String structureCode);
    List<BudgetAllocationStructure> findByStructure_CodeIn(List<String> structureCodes);
    boolean existsByStructure_Code(String structureCode);
    List<BudgetAllocationStructure> findByBudgetAllocationActiviteId(UUID allocationActiviteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select allocation from BudgetAllocationStructure allocation where allocation.id = :id")
    Optional<BudgetAllocationStructure> findByIdForUpdate(@Param("id") UUID id);
}