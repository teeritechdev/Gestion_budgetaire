package com.budget.backend.achat.repository;

import com.budget.backend.achat.entity.HistoriqueValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoriqueValidationRepository extends JpaRepository<HistoriqueValidation, UUID> {
    List<HistoriqueValidation> findByDemandeAchat_IdOrderByDateDecisionAsc(UUID daId);
    List<HistoriqueValidation> findByInstanceIdOrderByDateDecisionAsc(UUID instanceId);
}

