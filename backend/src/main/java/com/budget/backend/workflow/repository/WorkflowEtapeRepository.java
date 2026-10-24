package com.budget.backend.workflow.repository;

import com.budget.backend.workflow.entity.WorkflowEtape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkflowEtapeRepository extends JpaRepository<WorkflowEtape, UUID> {
}
