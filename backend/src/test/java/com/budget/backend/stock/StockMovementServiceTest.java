package com.budget.backend.stock;

import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.stock.entity.Article;
import com.budget.backend.stock.entity.MouvementStock;
import com.budget.backend.stock.repository.ArticleRepository;
import com.budget.backend.stock.repository.MouvementStockRepository;
import com.budget.backend.stock.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private MouvementStockRepository mouvementRepository;
    @Mock
    private jakarta.persistence.EntityManager em;
    @InjectMocks
    private StockMovementService service;

    private final UUID articleId = UUID.randomUUID();
    private final Utilisateur auteur = new Utilisateur();
    private Article article;

    @BeforeEach
    void setUp() {
        article = Article.builder().id(articleId).codeArticle("ART-1").quantiteStock(10)
                .prixUnitaireMoyenPondere(new BigDecimal("15000.00")).build();
        lenient().when(articleRepository.findByIdForUpdate(articleId)).thenReturn(Optional.of(article));
        lenient().when(articleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(mouvementRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void entreeRecalculeLePumpEtLeStock() {
        MouvementStock m = service.entreeStock(articleId, 10, new BigDecimal("17000"), "PV-1", auteur);

        assertEquals(20, article.getQuantiteStock());
        assertEquals(new BigDecimal("16000.00"), article.getPrixUnitaireMoyenPondere());
        assertEquals(10, m.getStockAnterieur());
        assertEquals(20, m.getNouveauStock());
        assertEquals(MouvementStock.ENTREE, m.getTypeMouvement());
    }

    @Test
    void entreeSansPrixLaissePumpInchange() {
        service.entreeStock(articleId, 5, null, "PV-2", auteur);

        assertEquals(15, article.getQuantiteStock());
        assertEquals(new BigDecimal("15000.00"), article.getPrixUnitaireMoyenPondere());
    }

    @Test
    void sortieDiminueLeStock() {
        MouvementStock m = service.sortieStock(articleId, 4, "BS-1", auteur);

        assertEquals(6, article.getQuantiteStock());
        assertEquals(10, m.getStockAnterieur());
        assertEquals(6, m.getNouveauStock());
    }

    @Test
    void sortieSuperieureAuStockEstRefuseeEtLeStockNeBougePas() {
        assertThrows(StockException.class, () -> service.sortieStock(articleId, 11, "BS-2", auteur));
        assertEquals(10, article.getQuantiteStock());
    }

    @Test
    void quantiteNulleOuNegativeEstRefusee() {
        assertThrows(StockException.class, () -> service.sortieStock(articleId, 0, "BS-3", auteur));
        assertThrows(StockException.class, () -> service.entreeStock(articleId, -1, null, "PV-3", auteur));
    }

    @Test
    void ajustementFixeLeStockComptEtEnregistreLEcart() {
        MouvementStock m = service.ajusterStock(articleId, 3, "INV-1", auteur);

        assertEquals(3, article.getQuantiteStock());
        assertEquals(7, m.getQuantite());
        assertEquals(10, m.getStockAnterieur());
        assertEquals(3, m.getNouveauStock());
    }

    @Test
    void referenceDocumentObligatoire() {
        assertThrows(StockException.class, () -> service.sortieStock(articleId, 1, " ", auteur));
    }
}
