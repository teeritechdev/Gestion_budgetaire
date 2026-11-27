package com.budget.backend.security.repository;

import com.budget.backend.security.entity.Structure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StructureRepository extends JpaRepository<Structure, String> {
    boolean existsByCode(String code);
}
