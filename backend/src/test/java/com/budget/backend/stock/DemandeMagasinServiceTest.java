package com.budget.backend.stock;

import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.stock.dto.DemandeMagasinResponseDto;
import com.budget.backend.stock.dto.DemandeMagasinServieDto;
import com.budget.backend.stock.dto.ValidationDecisionDto;
import com.budget.backend.stock.entity.Article;
import com.budget.backend.stock.entity.DemandeMagasin;
import com.budget.backend.stock.entity.LigneDemandeMagasin;
import com.budget.backend.stock.repository.DemandeMagasinRepository;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.DemandeMagasinService;
import com.budget.backend.stock.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandeMagasinServiceTest {

    @Mock
    private DemandeMagasinRepository repository;
    @Mock
    private StockMovementService stockMovementService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private com.budget.backend.stock.service.AuditHelper auditHelper;
    @InjectMocks
    private DemandeMagasinService service;

    private final UUID demandeId = UUID.randomUUID();
    private DemandeMagasin demande;
    private LigneDemandeMagasin ligne;

    @BeforeEach
    void setUp() {
        Article article = Article.builder().id(UUID.randomUUID()).codeArticle("ART-1").build();
        ligne = LigneDemandeMagasin.builder().id(UUID.randomUUID()).article(article)
                .quantiteDemande(15).quantiteServie(0).build();
        Structure structure = new Structure();
        structure.setId(UUID.randomUUID());
        demande = DemandeMagasin.builder().id(demandeId).numeroDemande("BS-2026-0001")
                .service(structure).entite(Entite.builder().code("UAB_VIE").build())
                .statut(DemandeMagasin.VALIDE_N1).build();
        ligne.setDemandeMagasin(demande);
        demande.getLignes().add(ligne);

        lenient().when(repository.findByIdForUpdate(demandeId)).thenReturn(Optional.of(demande));
        lenient().when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(currentUserService.utilisateurConnecte()).thenReturn(new Utilisateur());
    }

    private DemandeMagasinServieDto servir(int quantite) {
        DemandeMagasinServieDto.LigneServie ls = new DemandeMagasinServieDto.LigneServie();
        ls.setLigneId(ligne.getId());
        ls.setQuantiteServie(quantite);
        DemandeMagasinServieDto dto = new DemandeMagasinServieDto();
        dto.setLignes(List.of(ls));
        return dto;
    }

    private ValidationDecisionDto decision(String d) {
        ValidationDecisionDto dto = new ValidationDecisionDto();
        dto.setDecision(d);
        return dto;
    }

    @Test
    void serviceEnDeuxFoisCumuleLesQuantitesPuisSolde() {
        DemandeMagasinResponseDto r1 = service.servir(demandeId, servir(10));
        assertEquals(DemandeMagasin.PARTIELLEMENT_SERVIE, r1.getStatut());
        assertEquals(10, ligne.getQuantiteServie());

        DemandeMagasinResponseDto r2 = service.servir(demandeId, servir(5));
        assertEquals(DemandeMagasin.SERVIE, r2.getStatut());
        assertEquals(15, ligne.getQuantiteServie());
        verify(stockMovementService, times(2)).sortieStock(any(), anyInt(), eq("BS-2026-0001"), any());
    }

    @Test
    void servirAuDelaDuResteEstRefuseSansMouvement() {
        assertThrows(StockException.class, () -> service.servir(demandeId, servir(16)));
        verify(stockMovementService, never()).sortieStock(any(), anyInt(), anyString(), any());
        assertEquals(0, ligne.getQuantiteServie());
    }

    @Test
    void servirSansQuantitePositiveEstRefuse() {
        assertThrows(StockException.class, () -> service.servir(demandeId, servir(0)));
    }

    @Test
    void demandeSoumiseOuServieNePeutPasEtreServie() {
        demande.setStatut(DemandeMagasin.SOUMIS);
        assertThrows(StockException.class, () -> service.servir(demandeId, servir(1)));
        demande.setStatut(DemandeMagasin.SERVIE);
        assertThrows(StockException.class, () -> service.servir(demandeId, servir(1)));
    }

    @Test
    void validationApprouveEtRejet() {
        demande.setStatut(DemandeMagasin.SOUMIS);
        assertEquals(DemandeMagasin.VALIDE_N1, service.validerN1(demandeId, decision("approuve")).getStatut());

        demande.setStatut(DemandeMagasin.SOUMIS);
        assertEquals(DemandeMagasin.REJETEE, service.validerN1(demandeId, decision("REJETE")).getStatut());
    }

    @Test
    void validationRefuseeSiDecisionInvalideOuStatutIncorrect() {
        demande.setStatut(DemandeMagasin.SOUMIS);
        assertThrows(StockException.class, () -> service.validerN1(demandeId, decision("PEUTETRE")));

        demande.setStatut(DemandeMagasin.VALIDE_N1);
        assertThrows(StockException.class, () -> service.validerN1(demandeId, decision("APPROUVE")));
    }

    private Utilisateur utilisateur(Utilisateur manager) {
        Utilisateur u = new Utilisateur();
        u.setId(UUID.randomUUID());
        u.setManager(manager);
        return u;
    }

    @Test
    void demandeurNePeutPasValiderSaPropreDemande() {
        Utilisateur demandeur = utilisateur(null);
        demande.setDemandeur(demandeur);
        demande.setStatut(DemandeMagasin.SOUMIS);
        when(currentUserService.utilisateurConnecte()).thenReturn(demandeur);
        assertThrows(StockException.class, () -> service.validerN1(demandeId, decision("APPROUVE")));
        assertEquals(DemandeMagasin.SOUMIS, demande.getStatut());
    }

    @Test
    void seulLeResponsableDuDemandeurPeutValider() {
        Utilisateur responsable = utilisateur(null);
        demande.setDemandeur(utilisateur(responsable));
        demande.setStatut(DemandeMagasin.SOUMIS);

        when(currentUserService.utilisateurConnecte()).thenReturn(utilisateur(null));
        assertThrows(StockException.class, () -> service.validerN1(demandeId, decision("APPROUVE")));

        when(currentUserService.utilisateurConnecte()).thenReturn(responsable);
        assertEquals(DemandeMagasin.VALIDE_N1, service.validerN1(demandeId, decision("APPROUVE")).getStatut());
    }

    @Test
    void demandeurSansResponsableEstValidableParUnAutre() {
        demande.setDemandeur(utilisateur(null));
        demande.setStatut(DemandeMagasin.SOUMIS);
        when(currentUserService.utilisateurConnecte()).thenReturn(utilisateur(null));
        assertEquals(DemandeMagasin.VALIDE_N1, service.validerN1(demandeId, decision("APPROUVE")).getStatut());
    }

    @Test
    void demandeurNePeutPasServirSaPropreDemande() {
        Utilisateur demandeur = utilisateur(null);
        demande.setDemandeur(demandeur);
        when(currentUserService.utilisateurConnecte()).thenReturn(demandeur);
        assertThrows(StockException.class, () -> service.servir(demandeId, servir(1)));
        verify(stockMovementService, never()).sortieStock(any(), anyInt(), anyString(), any());
    }
}
