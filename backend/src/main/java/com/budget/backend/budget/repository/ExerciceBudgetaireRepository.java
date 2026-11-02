package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.ExerciceBudgetaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

@Repository
public interface ExerciceBudgetaireRepository extends JpaRepository<ExerciceBudgetaire, UUID>, JpaSpecificationExecutor<ExerciceBudgetaire> {
    Optional<ExerciceBudgetaire> findByAnnee(Integer annee);
    boolean existsByAnnee(Integer annee);

    // Verrou pessimiste : sérialise les allocations concurrentes sur la même enveloppe parente.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ExerciceBudgetaire e where e.id = :id")
    Optional<ExerciceBudgetaire> findByIdForUpdate(@Param("id") UUID id);
}
