package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.JournalMouvementsBudgetaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JournalMouvementsBudgetaireRepository extends JpaRepository<JournalMouvementsBudgetaire, UUID>, JpaSpecificationExecutor<JournalMouvementsBudgetaire> {
    List<JournalMouvementsBudgetaire> findByAnneeExercice(Integer anneeExercice);
    List<JournalMouvementsBudgetaire> findByAllocationStructureId(UUID allocationStructureId);
}