package com.budget.backend.stock.repository;

import com.budget.backend.security.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Lecture seule des utilisateurs pour le module Stocks : retrouve le compte lié à un salarié
 * (le responsable N+1 est un salarié ; seul son compte peut valider). Évite de modifier le module 1.
 */
@Repository
public interface StockUtilisateurRepository extends JpaRepository<Utilisateur, UUID> {
    Optional<Utilisateur> findBySalarie_Id(UUID salarieId);
}
