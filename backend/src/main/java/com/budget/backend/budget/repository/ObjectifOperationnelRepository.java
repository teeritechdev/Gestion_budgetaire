package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.ObjectifOperationnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObjectifOperationnelRepository extends JpaRepository<ObjectifOperationnel, UUID>, JpaSpecificationExecutor<ObjectifOperationnel> {
    Optional<ObjectifOperationnel> findByCodeOp(String codeOp);
    boolean existsByCodeOp(String codeOp);

    @Query("SELECT o FROM ObjectifOperationnel o WHERE o.objectifStrategique.id = :osId")
    List<ObjectifOperationnel> findByObjetifStrategiqueId(@Param("osId") UUID osId);
}