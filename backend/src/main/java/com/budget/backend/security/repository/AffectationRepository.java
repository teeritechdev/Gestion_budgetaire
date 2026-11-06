package com.budget.backend.security.repository;

import com.budget.backend.security.entity.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AffectationRepository extends JpaRepository<Affectation, UUID> {
    List<Affectation> findBySalarie_Id(UUID salarieId);

    boolean existsByStructure_Code(String structureCode);

    boolean existsByDirection_Id(UUID directionId);

    boolean existsByService_Id(UUID serviceId);

    void deleteBySalarie_Id(UUID salarieId);
}
