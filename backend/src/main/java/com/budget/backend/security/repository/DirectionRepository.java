package com.budget.backend.security.repository;

import com.budget.backend.security.entity.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface DirectionRepository extends JpaRepository<Direction, UUID> {
    boolean existsByCode(String code);
    Optional<Direction> findByCode(String code);
    boolean existsByStructure_Code(String structureCode);
    List<Direction> findByStructure_Code(String structureCode);
}
