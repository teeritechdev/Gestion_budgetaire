package com.budget.backend.abonnement;

import com.budget.backend.abonnement.dto.EcheanceResponseDto;
import com.budget.backend.abonnement.dto.EcheanceUpdateDto;
import com.budget.backend.abonnement.entity.Abonnement;
import com.budget.backend.abonnement.entity.EcheanceAbonnement;
import com.budget.backend.abonnement.repository.EcheanceAbonnementRepository;
import com.budget.backend.abonnement.service.EcheanceAbonnementService;
import com.budget.backend.achat.entity.BonCommande;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.repository.BonCommandeRepository;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.stock.service.AuditHelper;
import com.budget.backend.stock.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EcheanceAbonnementServiceTest {

    @Mock
    private EcheanceAbonnementRepository repository;
    @Mock
    private BonCommandeRepository bonCommandeRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AuditHelper auditHelper;
    @InjectMocks
    private EcheanceAbonnementService service;

    private Fournisseur securitas;
    private EcheanceAbonnement echeance;

    @BeforeEach
    void setUp() {
        securitas = Fournisseur.builder().id(UUID.randomUUID()).codeFournisseur("FOURN-SEC").build();
        Abonnement ab = Abonnement.builder().id(UUID.randomUUID()).codeAbonnement("ABN-SEC-2026").fournisseur(securitas).build();
        echeance = EcheanceAbonnement.builder().id(UUID.randomUUID()).abonnement(ab).numEcheance(2)
                .dateEcheance(LocalDate.of(2026, 2, 28)).montantAttenduTtc(new BigDecimal("590000")).statut("A_VENIR").build();
        lenient().when(repository.findByIdForUpdate(echeance.getId())).thenReturn(Optional.of(echeance));
        lenient().when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(currentUserService.utilisateurConnecte()).thenReturn(Utilisateur.builder().id(UUID.randomUUID()).build());
    }

    private BonCommande bc(Fournisseur f) {
        BonCommande bc = BonCommande.builder().id(UUID.randomUUID()).codeBc("BC-2026-0188").fournisseur(f).build();
        lenient().when(bonCommandeRepository.findById(bc.getId())).thenReturn(Optional.of(bc));
        return bc;
    }

    private EcheanceUpdateDto maj(String statut, UUID bcId, String reference, String montant) {
        EcheanceUpdateDto dto = new EcheanceUpdateDto();
        dto.setStatut(statut);
        dto.setBcId(bcId);
        dto.setReferenceFacture(reference);
        dto.setMontantFactureTtc(montant == null ? null : new BigDecimal(montant));
        return dto;
    }

    @Test
    void cycleCompletDuScenarioDeFevrier() {
        BonCommande bc = bc(securitas);

        EcheanceResponseDto r1 = service.modifier(echeance.getId(), maj("BC_GÉNÉRÉ", bc.getId(), null, null));
        assertEquals("BC_GENERE", r1.getStatut());
        assertEquals(bc.getId(), r1.getBcId());

        EcheanceResponseDto r2 = service.modifier(echeance.getId(), maj("FACTURE_REÇUE", null, "FAC-SEC-8841", "590000"));
        assertEquals("FACTURE_RECUE", r2.getStatut());
        assertEquals("FAC-SEC-8841", r2.getReferenceFacture());
        assertEquals(0, BigDecimal.ZERO.compareTo(r2.getEcartTtc()));

        EcheanceResponseDto r3 = service.modifier(echeance.getId(), maj("PAYÉ", null, null, null));
        assertEquals("PAYE", r3.getStatut());
    }

    @Test
    void ecartDeFacturationRepereLaSurconsommation() {
        echeance.setStatut("BC_GENERE");
        EcheanceResponseDto r = service.modifier(echeance.getId(), maj("FACTURE_RECUE", null, "FAC-1", "625000"));
        assertEquals(0, new BigDecimal("35000").compareTo(r.getEcartTtc()));
    }

    @Test
    void sautDEtapeInterdit() {
        assertThrows(AbonnementException.class, () -> service.modifier(echeance.getId(), maj("FACTURE_RECUE", null, "F", "1")));
        assertThrows(AbonnementException.class, () -> service.modifier(echeance.getId(), maj("PAYE", null, null, null)));
    }

    @Test
    void statutsFinauxNeBougentPlus() {
        echeance.setStatut("PAYE");
        assertThrows(AbonnementException.class, () -> service.modifier(echeance.getId(), maj("ANNULE", null, null, null)));
        echeance.setStatut("ANNULE");
        assertThrows(AbonnementException.class, () -> service.modifier(echeance.getId(), maj("BC_GENERE", UUID.randomUUID(), null, null)));
    }

    @Test
    void annulationPossibleAvantLaFacture() {
        assertEquals("ANNULE", service.modifier(echeance.getId(), maj("ANNULE", null, null, null)).getStatut());
        echeance.setStatut("BC_GENERE");
        assertEquals("ANNULE", service.modifier(echeance.getId(), maj("ANNULE", null, null, null)).getStatut());
        echeance.setStatut("FACTURE_RECUE");
        assertThrows(AbonnementException.class, () -> service.modifier(echeance.getId(), maj("ANNULE", null, null, null)));
    }

    @Test
    void donneesObligatoiresSelonLEtape() {
        assertThrows(AbonnementException.class, () -> service.modifier(echeance.getId(), maj("BC_GENERE", null, null, null)));

        echeance.setStatut("BC_GENERE");
        assertThrows(AbonnementException.class, () -> service.modifier(echeance.getId(), maj("FACTURE_RECUE", null, null, "590000")));
        assertThrows(AbonnementException.class, () -> service.modifier(echeance.getId(), maj("FACTURE_RECUE", null, "FAC-1", null)));
    }

    @Test
    void bonDeCommandeInconnuOuDUnAutreFournisseurRefuse() {
        assertThrows(AbonnementException.class, () -> service.modifier(echeance.getId(), maj("BC_GENERE", UUID.randomUUID(), null, null)));

        BonCommande autre = bc(Fournisseur.builder().id(UUID.randomUUID()).codeFournisseur("FOURN-AUTRE").build());
        assertThrows(AbonnementException.class, () -> service.modifier(echeance.getId(), maj("BC_GENERE", autre.getId(), null, null)));
        assertEquals("A_VENIR", echeance.getStatut());
    }

    @Test
    void echeanceInconnueRefusee() {
        assertThrows(AbonnementException.class, () -> service.modifier(UUID.randomUUID(), maj("ANNULE", null, null, null)));
    }
}
