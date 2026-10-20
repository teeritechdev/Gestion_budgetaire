package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.ActiviteCatalogue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActiviteCatalogueRepository extends JpaRepository<ActiviteCatalogue, UUID>, JpaSpecificationExecutor<ActiviteCatalogue> {
    Optional<ActiviteCatalogue> findByCodeActivite(String codeActivite);
    boolean existsByCodeActivite(String codeActivite);
    List<ActiviteCatalogue> findByObjectifOperationnelId(UUID opId);
}