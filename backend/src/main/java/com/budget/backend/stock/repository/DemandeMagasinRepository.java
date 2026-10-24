package com.budget.backend.stock.repository;

import com.budget.backend.stock.entity.DemandeMagasin;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemandeMagasinRepository extends JpaRepository<DemandeMagasin, UUID>, JpaSpecificationExecutor<DemandeMagasin> {
    boolean existsByNumeroDemande(String numeroDemande);

    // Verrou pessimiste : empêche deux services simultanés de dépasser la quantité demandée
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DemandeMagasin d where d.id = :id")
    Optional<DemandeMagasin> findByIdForUpdate(@Param("id") UUID id);
}
