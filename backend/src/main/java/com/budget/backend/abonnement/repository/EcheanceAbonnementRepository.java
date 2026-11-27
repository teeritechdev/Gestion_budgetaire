package com.budget.backend.abonnement.repository;

import com.budget.backend.abonnement.entity.EcheanceAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EcheanceAbonnementRepository extends JpaRepository<EcheanceAbonnement, UUID>, JpaSpecificationExecutor<EcheanceAbonnement> {
    List<EcheanceAbonnement> findByAbonnement_IdOrderByNumEcheanceAsc(UUID abonnementId);
    void deleteByAbonnement_Id(UUID abonnementId);
}
