package com.budget.backend.achat.repository;

import com.budget.backend.achat.entity.BonCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BonCommandeRepository extends JpaRepository<BonCommande, UUID>, JpaSpecificationExecutor<BonCommande> {
    Optional<BonCommande> findByCodeBc(String codeBc);
    boolean existsByCodeBc(String codeBc);
    boolean existsByDemandeAchat_Id(UUID daId);
}

