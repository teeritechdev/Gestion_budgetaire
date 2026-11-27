package com.budget.backend.budget.repository;

import com.budget.backend.budget.entity.RepartitionCspRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepartitionCspRuleRepository extends JpaRepository<RepartitionCspRule, UUID>, JpaSpecificationExecutor<RepartitionCspRule> {
    List<RepartitionCspRule> findAllByActiviteCspIdAndActiviteBeneficiaireId(
            UUID activiteCspId, UUID activiteBeneficiaireId);
    List<RepartitionCspRule> findAllByAllocationStructureIdAndActiviteBeneficiaireId(
            UUID allocationStructureId, UUID activiteBeneficiaireId);
}