package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.PlanStrategique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanStrategiqueRepository extends JpaRepository<PlanStrategique, UUID>, JpaSpecificationExecutor<PlanStrategique> {
    Optional<PlanStrategique> findByCodePlan(String codePlan);
    boolean existsByCodePlan(String codePlan);
}