package com.budget.backend.achat.repository;

import com.budget.backend.achat.entity.LigneBonCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LigneBonCommandeRepository extends JpaRepository<LigneBonCommande, UUID> {
    List<LigneBonCommande> findByBonCommande_Id(UUID bcId);
}

