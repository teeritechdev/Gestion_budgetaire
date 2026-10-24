package com.budget.backend.achat;

import com.budget.backend.achat.api.BudgetReservationApi;
import com.budget.backend.achat.api.BudgetReservationProvisoire;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.entity.JournalMouvementsBudgetaire;
import com.budget.backend.budget.repository.JournalMouvementsBudgetaireRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetReservationProvisoireTest {

    @Mock
    private EntityManager em;
    @Mock
    private JournalMouvementsBudgetaireRepository journalRepository;

    private BudgetReservationProvisoire service;
    private BudgetAllocationStructure ligne;
    private final UUID id = UUID.randomUUID();
    private final UUID auteur = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BudgetReservationProvisoire();
        ReflectionTestUtils.setField(service, "em", em);
        ReflectionTestUtils.setField(service, "journalRepository", journalRepository);
        ligne = BudgetAllocationStructure.builder().id(id)
                .montantVote(new BigDecimal("10000000")).montantModifie(new BigDecimal("2000000"))
                .montantEngage(new BigDecimal("1000000")).montantLiquide(new BigDecimal("500000")).build();
                lenient().when(em.find(BudgetAllocationStructure.class, id)).thenReturn(ligne);
    }

    @Test
    void leVerrouRelitLEtatSousVerrouApresUnFlush() {
        service.engager(id, new BigDecimal("1"), BudgetReservationApi.ENGAGEMENT_DA, "DA", 2026, auteur);

        InOrder ordre = inOrder(em);
        ordre.verify(em).flush();
        ordre.verify(em).refresh(ligne, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void disponibleEstCalculeEnJava() {
        // (10M + 2M) - (1M + 0,5M)
        assertEquals(new BigDecimal("10500000"), service.disponible(id));
    }

    @Test
    void engagerAugmenteLEngagementEtJournalise() {
        service.engager(id, new BigDecimal("2000000"), BudgetReservationApi.ENGAGEMENT_DA, "DA-2026-0001", 2026, auteur);

        assertEquals(new BigDecimal("3000000"), ligne.getMontantEngage());
        ArgumentCaptor<JournalMouvementsBudgetaire> captor = ArgumentCaptor.forClass(JournalMouvementsBudgetaire.class);
        verify(journalRepository).save(captor.capture());
        JournalMouvementsBudgetaire j = captor.getValue();
        assertEquals("ENGAGEMENT_DA", j.getTypeMouvement());
        assertEquals(new BigDecimal("1000000"), j.getMontantAnterieur());
        assertEquals(new BigDecimal("2000000"), j.getVariation());
        assertEquals(new BigDecimal("3000000"), j.getNouveauMontant());
        assertEquals("DA-2026-0001", j.getReferenceDocument());
    }

    @Test
    void engagerAuDelaDuDisponibleEstRefuseSansRienModifier() {
        assertThrows(AchatException.class, () -> service.engager(id, new BigDecimal("10500001"),
                BudgetReservationApi.ENGAGEMENT_DA, "DA", 2026, auteur));
        assertEquals(new BigDecimal("1000000"), ligne.getMontantEngage());
        verify(journalRepository, never()).save(any());
    }

    @Test
    void engagerExactementLeDisponibleEstAccepte() {
        service.engager(id, new BigDecimal("10500000"), BudgetReservationApi.ENGAGEMENT_DA, "DA", 2026, auteur);
        assertEquals(BigDecimal.ZERO.setScale(0), service.disponible(id).setScale(0));
    }

    @Test
    void libererDiminueLEngagementAvecVariationNegative() {
        service.liberer(id, new BigDecimal("400000"), BudgetReservationApi.ENGAGEMENT_BC, "BC-1", 2026, auteur);

        assertEquals(new BigDecimal("600000"), ligne.getMontantEngage());
        ArgumentCaptor<JournalMouvementsBudgetaire> captor = ArgumentCaptor.forClass(JournalMouvementsBudgetaire.class);
        verify(journalRepository).save(captor.capture());
        assertEquals(new BigDecimal("-400000"), captor.getValue().getVariation());
    }

    @Test
    void liquiderFaitPasserLeMontantDeEngageAILiquideSansChangerLeDisponible() {
        BigDecimal disponibleAvant = service.disponible(id);

        service.liquider(id, new BigDecimal("400000"), "FAC-2026-0034", 2026, auteur);

        assertEquals(new BigDecimal("600000"), ligne.getMontantEngage());   // 1 000 000 - 400 000
        assertEquals(new BigDecimal("900000"), ligne.getMontantLiquide());  // 500 000 + 400 000
        assertEquals(disponibleAvant, service.disponible(id));
        ArgumentCaptor<JournalMouvementsBudgetaire> captor = ArgumentCaptor.forClass(JournalMouvementsBudgetaire.class);
        verify(journalRepository).save(captor.capture());
        JournalMouvementsBudgetaire j = captor.getValue();
        assertEquals("LIQUIDATION_FACTURE", j.getTypeMouvement());
        assertEquals(new BigDecimal("500000"), j.getMontantAnterieur()); // colonne concernée : montant_liquide
        assertEquals(new BigDecimal("400000"), j.getVariation());
        assertEquals(new BigDecimal("900000"), j.getNouveauMontant());
        assertEquals("FAC-2026-0034", j.getReferenceDocument());
    }

    @Test
    void liquiderPlusQueLEngagementEstRefuseSansRienModifier() {
        assertThrows(AchatException.class, () -> service.liquider(id, new BigDecimal("1000001"), "FAC", 2026, auteur));
        assertEquals(new BigDecimal("1000000"), ligne.getMontantEngage());
        assertEquals(new BigDecimal("500000"), ligne.getMontantLiquide());
        verify(journalRepository, never()).save(any());
        assertThrows(AchatException.class, () -> service.liquider(id, BigDecimal.ZERO, "FAC", 2026, auteur));
    }

    @Test
    void libererPlusQueLEngagementEstRefuse() {
        assertThrows(AchatException.class, () -> service.liberer(id, new BigDecimal("1000001"),
                BudgetReservationApi.ENGAGEMENT_DA, "DA", 2026, auteur));
    }

    @Test
    void montantNulOuNegatifEtLigneInconnueRefuses() {
        assertThrows(AchatException.class, () -> service.engager(id, BigDecimal.ZERO, "X", "R", 2026, auteur));
        assertThrows(AchatException.class, () -> service.engager(UUID.randomUUID(), BigDecimal.TEN, "X", "R", 2026, auteur));
    }
}
