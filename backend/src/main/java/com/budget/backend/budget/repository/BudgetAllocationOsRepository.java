package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.BudgetAllocationOs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetAllocationOsRepository extends JpaRepository<BudgetAllocationOs, UUID>, JpaSpecificationExecutor<BudgetAllocationOs> {
    List<BudgetAllocationOs> findByExerciceBudgetaireId(UUID exerciceId);
    List<BudgetAllocationOs> findByObjectifStrategiqueId(UUID osId);
}