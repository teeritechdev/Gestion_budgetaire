package com.budget.backend.achat.repository;

import com.budget.backend.achat.entity.DemandeAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemandeAchatRepository extends JpaRepository<DemandeAchat, UUID>, JpaSpecificationExecutor<DemandeAchat> {
    Optional<DemandeAchat> findByCodeDossier(String codeDossier);
    boolean existsByCodeDossier(String codeDossier);
    long countByStatut(String statut);
}

