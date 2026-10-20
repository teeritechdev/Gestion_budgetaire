package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.ObjectifStrategique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObjectifStrategiqueRepository extends JpaRepository<ObjectifStrategique, UUID>, JpaSpecificationExecutor<ObjectifStrategique> {
    Optional<ObjectifStrategique> findByCodeOs(String codeOs);
    boolean existsByCodeOs(String codeOs);
}