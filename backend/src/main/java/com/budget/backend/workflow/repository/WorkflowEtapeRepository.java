package com.budget.backend.workflow.repository;

import com.budget.backend.workflow.entity.WorkflowEtape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowEtapeRepository extends JpaRepository<WorkflowEtape, UUID> {
    List<WorkflowEtape> findByWorkflow_IdOrderByNumEtapeAsc(UUID workflowId);
    Optional<WorkflowEtape> findByWorkflow_IdAndNumEtape(UUID workflowId, Integer numEtape);
    Optional<WorkflowEtape> findFirstByWorkflow_IdOrderByNumEtapeAsc(UUID workflowId);
}

