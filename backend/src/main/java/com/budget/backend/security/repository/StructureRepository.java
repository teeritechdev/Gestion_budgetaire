package com.budget.backend.security.repository;

import com.budget.backend.security.entity.Structure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StructureRepository extends JpaRepository<Structure, UUID> {
    Optional<Structure> findByCode(String code);
    List<Structure> findByEntiteCode(String entiteCode);
    List<Structure> findByParentId(UUID parentId);
}