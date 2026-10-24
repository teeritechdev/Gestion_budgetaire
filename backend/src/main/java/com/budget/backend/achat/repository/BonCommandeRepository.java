package com.budget.backend.achat.repository;

import com.budget.backend.achat.entity.BonCommande;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BonCommandeRepository extends JpaRepository<BonCommande, UUID>, JpaSpecificationExecutor<BonCommande> {
    boolean existsByCodeBc(String codeBc);
    boolean existsByDemandeAchatId(UUID daId);

    // Verrou pessimiste : deux factures simultanées sur le même BC ne peuvent pas dépasser son montant
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BonCommande b where b.id = :id")
    Optional<BonCommande> findByIdForUpdate(@Param("id") UUID id);
}
