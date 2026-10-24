package com.budget.backend.workflow.repository;

import com.budget.backend.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID>, JpaSpecificationExecutor<WorkflowDefinition> {
    Optional<WorkflowDefinition> findByCode(String code);
    boolean existsByCode(String code);
}
