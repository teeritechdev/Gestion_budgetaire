package com.budget.backend.abonnement.repository;

import com.budget.backend.abonnement.entity.EcheanceAbonnement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EcheanceAbonnementRepository extends JpaRepository<EcheanceAbonnement, UUID>, JpaSpecificationExecutor<EcheanceAbonnement> {
    List<EcheanceAbonnement> findByAbonnementIdOrderByNumEcheance(UUID abonnementId);

    List<EcheanceAbonnement> findByStatutAndDateEcheanceBetweenOrderByDateEcheance(String statut, LocalDate debut, LocalDate fin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EcheanceAbonnement e where e.id = :id")
    Optional<EcheanceAbonnement> findByIdForUpdate(@Param("id") UUID id);
}
