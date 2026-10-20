package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.LigneRevisionBudgetaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LigneRevisionBudgetaireRepository extends JpaRepository<LigneRevisionBudgetaire, UUID>, JpaSpecificationExecutor<LigneRevisionBudgetaire> {
    List<LigneRevisionBudgetaire> findByDemandeRevisionBudgetaireId(UUID revisionId);
}