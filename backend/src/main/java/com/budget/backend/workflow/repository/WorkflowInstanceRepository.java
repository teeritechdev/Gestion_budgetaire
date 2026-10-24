package com.budget.backend.workflow.repository;

import com.budget.backend.workflow.entity.WorkflowInstance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

    boolean existsByWorkflowCodeAndDossierId(String workflowCode, UUID dossierId);

    List<WorkflowInstance> findByDossierIdOrderByCreatedAtDesc(UUID dossierId);

    // Verrou pessimiste : deux décisions simultanées sur le même dossier sont sérialisées
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from WorkflowInstance i where i.workflow.code = :code and i.dossierId = :dossierId")
    Optional<WorkflowInstance> findForUpdate(@Param("code") String code, @Param("dossierId") UUID dossierId);
}
