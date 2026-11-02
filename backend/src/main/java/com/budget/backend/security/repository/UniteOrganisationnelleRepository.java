package com.budget.backend.security.repository;

import com.budget.backend.security.entity.UniteOrganisationnelle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UniteOrganisationnelleRepository extends JpaRepository<UniteOrganisationnelle, UUID> {
    Optional<UniteOrganisationnelle> findByCode(String code);
    List<UniteOrganisationnelle> findByStructureCode(String structureCode);
    List<UniteOrganisationnelle> findByParent_Id(UUID parentId);
    List<UniteOrganisationnelle> findByParent_IdIn(List<UUID> parentIds);

    @Query("select count(distinct u.structure.code) from UniteOrganisationnelle u")
    long countDistinctStructures();
}
