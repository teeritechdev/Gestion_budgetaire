package com.budget.backend.achat.repository;

import com.budget.backend.achat.entity.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, UUID>, JpaSpecificationExecutor<Fournisseur> {
    boolean existsByCodeFournisseur(String codeFournisseur);
    boolean existsByIfu(String ifu);
}
