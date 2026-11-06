package com.budget.backend.security.repository;

import com.budget.backend.security.entity.PhotoProfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PhotoProfilRepository extends JpaRepository<PhotoProfil, UUID> {
}
