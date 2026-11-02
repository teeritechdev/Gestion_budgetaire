package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.EntiteStatistiqueCle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface EntiteStatistiqueCleRepository extends JpaRepository<EntiteStatistiqueCle, UUID>, JpaSpecificationExecutor<EntiteStatistiqueCle> {
    boolean existsByAnneeExerciceAndMoisAndEntiteCode(Integer anneeExercice, Integer mois, String entiteCode);
    boolean existsByAnneeExerciceAndMoisAndEntiteCodeAndIdNot(Integer anneeExercice, Integer mois, String entiteCode, UUID id);
    Optional<EntiteStatistiqueCle> findFirstByEntiteCodeAndMoisIsNotNullOrderByAnneeExerciceDescMoisDesc(String entiteCode);
}