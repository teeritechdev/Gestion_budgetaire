package com.budget.backend.security.repository;

import com.budget.backend.security.entity.ServiceOrganisationnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceOrganisationnelRepository extends JpaRepository<ServiceOrganisationnel, UUID> {
    boolean existsByCode(String code);
    Optional<ServiceOrganisationnel> findByCode(String code);
    boolean existsByDirection_Id(UUID directionId);
    List<ServiceOrganisationnel> findByDirection_Id(UUID directionId);
}
