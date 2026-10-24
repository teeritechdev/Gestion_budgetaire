package com.budget.backend.abonnement;

import com.budget.backend.abonnement.dto.AbonnementRequestDto;
import com.budget.backend.abonnement.dto.AbonnementResponseDto;
import com.budget.backend.abonnement.dto.AlertesResponseDto;
import com.budget.backend.abonnement.dto.EcheanceResponseDto;
import com.budget.backend.abonnement.entity.Abonnement;
import com.budget.backend.abonnement.entity.EcheanceAbonnement;
import com.budget.backend.abonnement.repository.AbonnementRepository;
import com.budget.backend.abonnement.repository.EcheanceAbonnementRepository;
import com.budget.backend.abonnement.service.AbonnementService;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.repository.FournisseurRepository;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.stock.service.AuditHelper;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.NumeroDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbonnementServiceTest {

    @Mock
    private AbonnementRepository repository;
    @Mock
    private EcheanceAbonnementRepository echeanceRepository;
    @Mock
    private FournisseurRepository fournisseurRepository;
    @Mock
    private BudgetAllocationStructureRepository allocationRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private NumeroDocumentService numeroDocumentService;
    @Mock
    private AuditHelper auditHelper;
    @InjectMocks
    private AbonnementService service;

    private Fournisseur fournisseur;
    private BudgetAllocationStructure activite;
    private Utilisateur responsable;
    // "Base" des échéances : le mock lit et écrit dans cette liste
    private final List<EcheanceAbonnement> echeances = new ArrayList<>();

    @BeforeEach
    void setUp() {
        fournisseur = Fournisseur.builder().id(UUID.randomUUID()).codeFournisseur("FOURN-1").raisonSociale("SECURITAS").actif(true).build();
        activite = BudgetAllocationStructure.builder().id(UUID.randomUUID()).build();
        responsable = Utilisateur.builder().id(UUID.randomUUID()).build();

        lenient().when(fournisseurRepository.findById(fournisseur.getId())).thenReturn(Optional.of(fournisseur));
        lenient().when(allocationRepository.findById(activite.getId())).thenReturn(Optional.of(activite));
        lenient().when(utilisateurRepository.findById(responsable.getId())).thenReturn(Optional.of(responsable));
        lenient().when(currentUserService.utilisateurConnecte()).thenReturn(responsable);
        lenient().when(numeroDocumentService.prochainCodeAbonnement()).thenReturn("ABN-2026-0001");
        lenient().when(repository.saveAndFlush(any())).thenAnswer(i -> {
            Abonnement a = i.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });
        lenient().when(echeanceRepository.findByAbonnementIdOrderByNumEcheance(any())).thenAnswer(i -> new ArrayList<>(echeances));
        lenient().when(echeanceRepository.saveAll(any())).thenAnswer(i -> {
            Iterable<EcheanceAbonnement> nouvelles = i.getArgument(0);
            nouvelles.forEach(echeances::add);
            return nouvelles;
        });
    }

    private AbonnementRequestDto requete() {
        AbonnementRequestDto dto = new AbonnementRequestDto();
        dto.setLibelle("Gardiennage et Sécurité - Siège");
        dto.setFournisseurId(fournisseur.getId());
        dto.setActiviteId(activite.getId());
        dto.setPeriodicite("MENSUEL");
        dto.setJourFacturation(31);
        dto.setMontantEcheanceHt(new BigDecimal("500000"));
        dto.setMontantEcheanceTtc(new BigDecimal("590000"));
        dto.setDateDebut(LocalDate.of(2026, 1, 1));
        dto.setDateFin(LocalDate.of(2026, 12, 31));
        dto.setTaciteReconduction(true);
        dto.setPreavisJours(60);
        dto.setResponsableId(responsable.getId());
        return dto;
    }

    private Abonnement abonnementExistant(String statut) {
        Abonnement ab = Abonnement.builder().id(UUID.randomUUID()).codeAbonnement("ABN-X").libelle("x").fournisseur(fournisseur)
                .activite(activite).periodicite("MENSUEL").jourFacturation(31)
                .montantEcheanceHt(new BigDecimal("500000")).montantEcheanceTtc(new BigDecimal("590000"))
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).taciteReconduction(true)
                .preavisJours(60).statut(statut).responsable(responsable).build();
        lenient().when(repository.findByIdForUpdate(ab.getId())).thenReturn(Optional.of(ab));
        return ab;
    }

    private EcheanceAbonnement echeance(Abonnement ab, int num, LocalDate date, String statut) {
        EcheanceAbonnement e = EcheanceAbonnement.builder().id(UUID.randomUUID()).abonnement(ab).numEcheance(num)
                .dateEcheance(date).montantAttenduTtc(ab.getMontantEcheanceTtc()).statut(statut).build();
        echeances.add(e);
        return e;
    }

    // --- Pagination des échéances ---

    @Test
    @SuppressWarnings("unchecked")
    void echeancesPagineesRenvoientLeFormatCommunEtPlafonnentLaTaille() {
        Abonnement ab = abonnementExistant("ACTIF");
        when(repository.existsById(ab.getId())).thenReturn(true);
        List<EcheanceAbonnement> deux = List.of(echeance(ab, 1, LocalDate.of(2026, 1, 31), "A_VENIR"),
                echeance(ab, 2, LocalDate.of(2026, 2, 28), "A_VENIR"));
        when(echeanceRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(deux, org.springframework.data.domain.PageRequest.of(1, 2), 12));

        var r = service.echeances(ab.getId(), "PAYÉ", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                org.springframework.data.domain.PageRequest.of(1, 5000));

        assertEquals(2, r.getContent().size());
        assertEquals(1, r.getNumber());
        assertEquals(12, r.getTotalElements());
        assertEquals(6, r.getTotalPages());
        assertFalse(r.isFirst());
        assertFalse(r.isLast());
        var captor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(echeanceRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), captor.capture());
        assertEquals(200, captor.getValue().getPageSize()); // 5000 demandé, plafonné
        assertEquals(1, captor.getValue().getPageNumber());
    }

    @Test
    void echeancesDUnAbonnementInconnuRefusees() {
        assertThrows(AbonnementException.class, () -> service.echeances(UUID.randomUUID(), null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 20)));
    }

    // --- Création ---

    @Test
    void creationActiveGenereLesDouzeEcheancesDuScenario() {
        AbonnementResponseDto r = service.creer(requete());

        assertEquals("ABN-2026-0001", r.getCodeAbonnement());
        assertEquals("ACTIF", r.getStatut());
        assertEquals(12, echeances.size());
        assertEquals(LocalDate.of(2026, 2, 28), echeances.get(1).getDateEcheance());
        assertEquals(new BigDecimal("590000"), echeances.get(0).getMontantAttenduTtc());
        assertEquals("A_VENIR", echeances.get(0).getStatut());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), echeances.stream().map(EcheanceAbonnement::getNumEcheance).toList());
        // préavis de 60 jours pour une fin au 31/12/2026 : le PDF cite le 31 octobre 2026
        assertEquals(LocalDate.of(2026, 10, 31), r.getDateLimitePreavis());
    }

    @Test
    void creationEnBrouillonNeGenerePasDEcheances() {
        AbonnementRequestDto dto = requete();
        dto.setStatut("brouillon");
        assertEquals("BROUILLON", service.creer(dto).getStatut());
        assertTrue(echeances.isEmpty());
    }

    @Test
    void statutDeCreationInterditEtDonneesInvalidesRefusees() {
        AbonnementRequestDto dto = requete();
        dto.setStatut("RESILIE");
        assertThrows(AbonnementException.class, () -> service.creer(dto));

        AbonnementRequestDto periodicite = requete();
        periodicite.setPeriodicite("QUOTIDIEN");
        assertThrows(AbonnementException.class, () -> service.creer(periodicite));

        AbonnementRequestDto dates = requete();
        dates.setDateFin(LocalDate.of(2025, 12, 31));
        assertThrows(AbonnementException.class, () -> service.creer(dates));

        AbonnementRequestDto montants = requete();
        montants.setMontantEcheanceTtc(new BigDecimal("400000"));
        assertThrows(AbonnementException.class, () -> service.creer(montants));
        assertTrue(echeances.isEmpty());
    }

    @Test
    void fournisseurSuspenduRefuse() {
        fournisseur.setActif(false);
        assertThrows(AbonnementException.class, () -> service.creer(requete()));
    }

    @Test
    void codeDejaUtiliseRefuse() {
        AbonnementRequestDto dto = requete();
        dto.setCodeAbonnement("ABN-SEC-2026");
        when(repository.existsByCodeAbonnement("ABN-SEC-2026")).thenReturn(true);
        assertThrows(AbonnementException.class, () -> service.creer(dto));
    }

    @Test
    void lesAccentsDuMrdSontAcceptesEnEntree() {
        assertEquals("RESILIE", AbonnementService.normaliser("RÉSILIÉ"));
        assertEquals("BC_GENERE", AbonnementService.normaliser("bc_généré"));
        assertEquals("PAYE", AbonnementService.normaliser(" payé "));
        assertNull(AbonnementService.normaliser(null));
    }

    // --- Génération / reconduction ---

    @Test
    void generationEstIdempotenteEtContinueLaNumerotation() {
        Abonnement ab = abonnementExistant("ACTIF");
        echeance(ab, 1, LocalDate.of(2026, 1, 31), "PAYE");
        echeance(ab, 2, LocalDate.of(2026, 2, 28), "A_VENIR");

        List<EcheanceResponseDto> r = service.genererEcheances(ab.getId());
        assertEquals(12, r.size());
        assertEquals(3, echeances.get(2).getNumEcheance());
        assertEquals(LocalDate.of(2026, 3, 31), echeances.get(2).getDateEcheance());

        service.genererEcheances(ab.getId()); // second appel : rien de nouveau
        assertEquals(12, echeances.size());
        assertEquals("PAYE", echeances.get(0).getStatut());
    }

    @Test
    void generationRefuseeSiLeContratNEstPasActif() {
        assertThrows(AbonnementException.class, () -> service.genererEcheances(abonnementExistant("BROUILLON").getId()));
        assertThrows(AbonnementException.class, () -> service.genererEcheances(abonnementExistant("SUSPENDU").getId()));
    }

    @Test
    void reconductionProlongeDUnAnEtGenereDouzeNouvellesEcheances() {
        Abonnement ab = abonnementExistant("ACTIF");
        service.genererEcheances(ab.getId());
        assertEquals(12, echeances.size());

        AbonnementResponseDto r = service.reconduire(ab.getId());

        assertEquals(LocalDate.of(2027, 12, 31), r.getDateFin());
        assertEquals(24, echeances.size());
        assertEquals(13, echeances.get(12).getNumEcheance());
        assertEquals(LocalDate.of(2027, 1, 31), echeances.get(12).getDateEcheance());
    }

    @Test
    void reconductionRefuseeSiLeContratNEstPasActif() {
        assertThrows(AbonnementException.class, () -> service.reconduire(abonnementExistant("RESILIE").getId()));
    }

    // --- Modification ---

    @Test
    void resiliationAnnuleLesEcheancesAVenirMaisPasCellesDejaTraitees() {
        Abonnement ab = abonnementExistant("ACTIF");
        EcheanceAbonnement payee = echeance(ab, 1, LocalDate.of(2026, 1, 31), "PAYE");
        EcheanceAbonnement avenir = echeance(ab, 2, LocalDate.of(2026, 2, 28), "A_VENIR");

        AbonnementRequestDto dto = requete();
        dto.setStatut("RÉSILIÉ");
        AbonnementResponseDto r = service.modifier(ab.getId(), dto);

        assertEquals("RESILIE", r.getStatut());
        assertEquals("PAYE", payee.getStatut());
        assertEquals("ANNULE", avenir.getStatut());
        assertEquals(2, echeances.size()); // plus aucune génération
    }

    @Test
    void transitionsDeStatutInterditesRefusees() {
        Abonnement ab = abonnementExistant("ACTIF");
        AbonnementRequestDto dto = requete();
        dto.setStatut("BROUILLON");
        assertThrows(AbonnementException.class, () -> service.modifier(ab.getId(), dto));

        AbonnementRequestDto expire = requete();
        expire.setStatut("EXPIRE");
        assertThrows(AbonnementException.class, () -> service.modifier(ab.getId(), expire));
    }

    @Test
    void abonnementResilieOuExpireNonModifiable() {
        assertThrows(AbonnementException.class, () -> service.modifier(abonnementExistant("RESILIE").getId(), requete()));
        assertThrows(AbonnementException.class, () -> service.modifier(abonnementExistant("EXPIRE").getId(), requete()));
    }

    @Test
    void activationDUnBrouillonGenereLEcheancier() {
        Abonnement ab = abonnementExistant("BROUILLON");
        AbonnementRequestDto dto = requete();
        dto.setStatut("ACTIF");

        service.modifier(ab.getId(), dto);

        assertEquals(12, echeances.size());
    }

    @Test
    void nouveauMontantMetAJourLesEcheancesAVenirSeulement() {
        Abonnement ab = abonnementExistant("ACTIF");
        EcheanceAbonnement facturee = echeance(ab, 1, LocalDate.of(2026, 1, 31), "FACTURE_RECUE");
        EcheanceAbonnement avenir = echeance(ab, 2, LocalDate.of(2026, 2, 28), "A_VENIR");

        AbonnementRequestDto dto = requete();
        dto.setMontantEcheanceHt(new BigDecimal("550000"));
        dto.setMontantEcheanceTtc(new BigDecimal("649000"));
        service.modifier(ab.getId(), dto);

        assertEquals(new BigDecimal("649000"), avenir.getMontantAttenduTtc());
        assertEquals(new BigDecimal("590000"), facturee.getMontantAttenduTtc());
    }

    @Test
    void changerLeRythmeRefuseUneFoisUneEcheanceTraitee() {
        Abonnement ab = abonnementExistant("ACTIF");
        echeance(ab, 1, LocalDate.of(2026, 1, 31), "BC_GENERE");
        AbonnementRequestDto dto = requete();
        dto.setPeriodicite("TRIMESTRIEL");

        assertThrows(AbonnementException.class, () -> service.modifier(ab.getId(), dto));
        verify(echeanceRepository, never()).deleteAll(any());
    }

    @Test
    void changerLeRythmeRecalculeLeCalendrierTantQueToutEstAVenir() {
        Abonnement ab = abonnementExistant("ACTIF");
        List<EcheanceAbonnement> anciennes = List.of(echeance(ab, 1, LocalDate.of(2026, 1, 31), "A_VENIR"),
                echeance(ab, 2, LocalDate.of(2026, 2, 28), "A_VENIR"));
        doAnswer(i -> {
            echeances.removeAll(i.<List<EcheanceAbonnement>>getArgument(0));
            return null;
        }).when(echeanceRepository).deleteAll(any());

        AbonnementRequestDto dto = requete();
        dto.setPeriodicite("TRIMESTRIEL");
        service.modifier(ab.getId(), dto);

        verify(echeanceRepository).deleteAll(anciennes);
        assertEquals(4, echeances.size()); // 31/01, 30/04, 31/07, 31/10
        assertEquals(LocalDate.of(2026, 4, 30), echeances.get(1).getDateEcheance());
    }

    @Test
    void raccourcirLaFinAnnuleLesEcheancesAVenirAuDela() {
        Abonnement ab = abonnementExistant("ACTIF");
        EcheanceAbonnement mai = echeance(ab, 5, LocalDate.of(2026, 5, 31), "A_VENIR");
        EcheanceAbonnement juin = echeance(ab, 6, LocalDate.of(2026, 6, 30), "A_VENIR");

        AbonnementRequestDto dto = requete();
        dto.setDateFin(LocalDate.of(2026, 5, 31));
        service.modifier(ab.getId(), dto);

        assertEquals("A_VENIR", mai.getStatut());
        assertEquals("ANNULE", juin.getStatut());
    }

    // --- Fin de contrat (tâche planifiée) ---

    @Test
    void finDeContratAvecTaciteReconductionProlongeEtGenere() {
        Abonnement ab = abonnementExistant("ACTIF");
        ab.setTaciteReconduction(true);
        service.genererEcheances(ab.getId()); // l'échéancier 2026 existe déjà
        when(repository.findByStatutAndDateFinBefore("ACTIF", LocalDate.of(2027, 1, 2))).thenReturn(List.of(ab));

        int n = service.traiterFinsDeContrat(LocalDate.of(2027, 1, 2));

        assertEquals(1, n);
        assertEquals("ACTIF", ab.getStatut());
        assertEquals(LocalDate.of(2027, 12, 31), ab.getDateFin());
        assertEquals(24, echeances.size()); // les 12 échéances de 2027 s'ajoutent à celles de 2026
        assertEquals(13, echeances.get(12).getNumEcheance());
        assertEquals(LocalDate.of(2027, 1, 31), echeances.get(12).getDateEcheance());
    }

    @Test
    void finDeContratSansReconductionExpire() {
        Abonnement ab = abonnementExistant("ACTIF");
        ab.setTaciteReconduction(false);
        when(repository.findByStatutAndDateFinBefore("ACTIF", LocalDate.of(2027, 1, 2))).thenReturn(List.of(ab));

        service.traiterFinsDeContrat(LocalDate.of(2027, 1, 2));

        assertEquals("EXPIRE", ab.getStatut());
        assertTrue(echeances.isEmpty());
    }

    @Test
    void reconductionTacitePlusieursAnsDeRetardRattrapeTout() {
        Abonnement ab = abonnementExistant("ACTIF");
        when(repository.findByStatutAndDateFinBefore("ACTIF", LocalDate.of(2028, 2, 1))).thenReturn(List.of(ab));

        service.traiterFinsDeContrat(LocalDate.of(2028, 2, 1));

        assertEquals(LocalDate.of(2028, 12, 31), ab.getDateFin());
    }

    // --- Alertes ---

    @Test
    void alertePreavisLeJourExactDuScenario() {
        Abonnement ab = abonnementExistant("ACTIF"); // fin 31/12/2026, préavis 60 j => limite 31/10/2026 (date du PDF)
        when(repository.findByStatut("ACTIF")).thenReturn(List.of(ab));

        assertTrue(service.alertes(10, LocalDate.of(2026, 10, 30)).getPreavisAtteints().isEmpty());

        AlertesResponseDto atteint = service.alertes(10, LocalDate.of(2026, 10, 31));
        assertEquals(1, atteint.getPreavisAtteints().size());
        assertEquals(LocalDate.of(2026, 10, 31), atteint.getPreavisAtteints().get(0).getDateLimitePreavis());
        assertEquals(61, atteint.getPreavisAtteints().get(0).getJoursAvantFin());
        assertTrue(atteint.getPreavisAtteints().get(0).getTaciteReconduction());

        // après la fin du contrat, plus d'alerte
        assertTrue(service.alertes(10, LocalDate.of(2027, 1, 1)).getPreavisAtteints().isEmpty());
    }

    @Test
    void alerteEcheancesProchesIgnoreLesContratsNonActifs() {
        Abonnement actif = abonnementExistant("ACTIF");
        Abonnement suspendu = abonnementExistant("SUSPENDU");
        EcheanceAbonnement e1 = echeance(actif, 2, LocalDate.of(2026, 2, 28), "A_VENIR");
        EcheanceAbonnement e2 = echeance(suspendu, 2, LocalDate.of(2026, 2, 28), "A_VENIR");
        when(echeanceRepository.findByStatutAndDateEcheanceBetweenOrderByDateEcheance("A_VENIR",
                LocalDate.of(2026, 2, 20), LocalDate.of(2026, 3, 2))).thenReturn(List.of(e1, e2));

        AlertesResponseDto r = service.alertes(10, LocalDate.of(2026, 2, 20));

        assertEquals(1, r.getEcheancesProches().size());
        assertEquals(e1.getId(), r.getEcheancesProches().get(0).getId());
    }
}
