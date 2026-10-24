package com.budget.backend.abonnement.repository;

import com.budget.backend.abonnement.entity.Abonnement;
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
public interface AbonnementRepository extends JpaRepository<Abonnement, UUID>, JpaSpecificationExecutor<Abonnement> {
    boolean existsByCodeAbonnement(String codeAbonnement);

    // Verrou pessimiste : sérialise génération d'échéances / modifications concurrentes d'un même contrat
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Abonnement a where a.id = :id")
    Optional<Abonnement> findByIdForUpdate(@Param("id") UUID id);

    List<Abonnement> findByStatutAndDateFinBefore(String statut, LocalDate date);

    List<Abonnement> findByStatut(String statut);
}
