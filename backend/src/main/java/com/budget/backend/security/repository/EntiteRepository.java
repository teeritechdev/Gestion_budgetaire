package com.budget.backend.security.repository;

import com.budget.backend.security.entity.Entite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntiteRepository extends JpaRepository<Entite, String> {
    boolean existsByCode(String code);
}
