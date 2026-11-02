package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.DemandeRevisionBudgetaire;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemandeRevisionBudgetaireRepository extends JpaRepository<DemandeRevisionBudgetaire, UUID>, JpaSpecificationExecutor<DemandeRevisionBudgetaire> {
    Optional<DemandeRevisionBudgetaire> findByNumeroDemande(String numeroDemande);
    List<DemandeRevisionBudgetaire> findByAnneeExercice(Integer anneeExercice);
    List<DemandeRevisionBudgetaire> findByStatut(String statut);
    Optional<DemandeRevisionBudgetaire> findByIdAndDemandeurId(UUID id, UUID demandeurId);
    long countByStatut(String statut);
    long countByDemandeurIdAndStatut(UUID demandeurId, String statut);

    @Query("""
            select demande from DemandeRevisionBudgetaire demande
            where demande.demandeurId = :demandeurId
              and (:anneeExercice is null or demande.anneeExercice = :anneeExercice)
              and (:statut is null or demande.statut = :statut)
            """)
    List<DemandeRevisionBudgetaire> findByDemandeurAndFilters(
            @Param("demandeurId") UUID demandeurId,
            @Param("anneeExercice") Integer anneeExercice,
            @Param("statut") String statut);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select demande from DemandeRevisionBudgetaire demande where demande.id = :id")
    Optional<DemandeRevisionBudgetaire> findByIdForUpdate(@Param("id") UUID id);
}