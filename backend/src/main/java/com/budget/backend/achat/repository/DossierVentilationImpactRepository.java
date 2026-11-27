package com.budget.backend.achat.repository;

import com.budget.backend.achat.entity.DossierVentilationImpact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DossierVentilationImpactRepository extends JpaRepository<DossierVentilationImpact, UUID> {
    List<DossierVentilationImpact> findByDemandeAchat_IdOrderByCreatedAtAsc(UUID daId);
    void deleteByDemandeAchat_Id(UUID daId);
}

