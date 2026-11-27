package com.budget.backend.security.repository;

import com.budget.backend.security.entity.JetonReinitialisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JetonReinitialisationRepository extends JpaRepository<JetonReinitialisation, UUID> {

    Optional<JetonReinitialisation> findByEmpreinte(String empreinte);

    /** Un nouveau jeton annule les précédents encore valides du même compte. */
    @Modifying
    @Query("update JetonReinitialisation j set j.utiliseLe = :maintenant "
            + "where j.utilisateurId = :utilisateurId and j.utiliseLe is null")
    int annulerJetonsActifs(UUID utilisateurId, Instant maintenant);

    @Modifying
    @Query("delete from JetonReinitialisation j where j.expireLe < :limite")
    int supprimerExpiresAvant(Instant limite);
}
