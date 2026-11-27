package com.budget.backend.workflow.repository;

import com.budget.backend.workflow.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID>, JpaSpecificationExecutor<WorkflowInstance> {
    Optional<WorkflowInstance> findByDossierId(UUID dossierId);
    Optional<WorkflowInstance> findByReferenceDossier(String referenceDossier);
    boolean existsByDossierId(UUID dossierId);
}

