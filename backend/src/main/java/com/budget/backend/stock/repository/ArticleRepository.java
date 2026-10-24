package com.budget.backend.stock.repository;

import com.budget.backend.stock.entity.Article;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID>, JpaSpecificationExecutor<Article> {
    Optional<Article> findByCodeArticle(String codeArticle);
    boolean existsByCodeArticle(String codeArticle);

    // Verrou pessimiste : évite deux sorties simultanées qui rendraient le stock négatif
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Article a where a.id = :id")
    Optional<Article> findByIdForUpdate(@Param("id") UUID id);
}
