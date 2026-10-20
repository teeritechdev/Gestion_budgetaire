package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.DemandeRevisionBudgetaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemandeRevisionBudgetaireRepository extends JpaRepository<DemandeRevisionBudgetaire, UUID>, JpaSpecificationExecutor<DemandeRevisionBudgetaire> {
    Optional<DemandeRevisionBudgetaire> findByNumeroDemande(String numeroDemande);
    List<DemandeRevisionBudgetaire> findByAnneeExercice(Integer anneeExercice);
    List<DemandeRevisionBudgetaire> findByStatut(String statut);
}