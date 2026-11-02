package com.budget.backend.security.repository;

import com.budget.backend.security.entity.Salarie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalarieRepository extends JpaRepository<Salarie, UUID> {
    Optional<Salarie> findByMatricule(String matricule);
    Optional<Salarie> findByEmail(String email);
    boolean existsByMatricule(String matricule);
    boolean existsByMatriculeAndIdNot(String matricule, UUID id);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    boolean existsByService_Id(UUID serviceId);
    boolean existsByManager_Id(UUID managerId);
    List<Salarie> findByService_Id(UUID serviceId);
}
