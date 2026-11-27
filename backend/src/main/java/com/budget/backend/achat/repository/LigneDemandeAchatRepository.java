package com.budget.backend.achat.repository;

import com.budget.backend.achat.entity.LigneDemandeAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LigneDemandeAchatRepository extends JpaRepository<LigneDemandeAchat, UUID> {
    List<LigneDemandeAchat> findByDemandeAchat_Id(UUID daId);
}

