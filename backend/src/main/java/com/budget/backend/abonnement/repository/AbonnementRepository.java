package com.budget.backend.abonnement.repository;

import com.budget.backend.abonnement.entity.Abonnement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AbonnementRepository extends JpaRepository<Abonnement, UUID>, JpaSpecificationExecutor<Abonnement> {
    Optional<Abonnement> findByCodeAbonnement(String codeAbonnement);
    boolean existsByCodeAbonnement(String codeAbonnement);
}

