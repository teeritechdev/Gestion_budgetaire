package com.budget.backend.achat.repository;

import com.budget.backend.achat.entity.DemandeAchat;
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
public interface DemandeAchatRepository extends JpaRepository<DemandeAchat, UUID>, JpaSpecificationExecutor<DemandeAchat> {
    boolean existsByCodeDossier(String codeDossier);

    // Verrou pessimiste : sérialise les validations / modifications concurrentes d'un même dossier
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DemandeAchat d where d.id = :id")
    Optional<DemandeAchat> findByIdForUpdate(@Param("id") UUID id);
}
