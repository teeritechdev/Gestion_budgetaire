package com.budget.backend.achat;

import com.budget.backend.achat.api.BudgetReservationApi;
import com.budget.backend.achat.dto.BonCommandeCreateDto;
import com.budget.backend.achat.dto.BonCommandeResponseDto;
import com.budget.backend.achat.dto.LigneBCRequestDto;
import com.budget.backend.achat.entity.BonCommande;
import com.budget.backend.achat.entity.DemandeAchat;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.entity.LigneDemandeAchat;
import com.budget.backend.achat.repository.BonCommandeRepository;
import com.budget.backend.achat.repository.DemandeAchatRepository;
import com.budget.backend.achat.repository.FournisseurRepository;
import com.budget.backend.achat.service.BonCommandeService;
import com.budget.backend.achat.service.DemandeAchatService;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.NumeroDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BonCommandeServiceTest {

    @Mock
    private BonCommandeRepository repository;
    @Mock
    private DemandeAchatRepository demandeAchatRepository;
    @Mock
    private FournisseurRepository fournisseurRepository;
    @Mock
    private com.budget.backend.stock.repository.ArticleRepository articleRepository;
    @Mock
    private DemandeAchatService demandeAchatService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private NumeroDocumentService numeroDocumentService;
    @Mock
    private BudgetReservationApi budgetApi;
    @Mock
    private com.budget.backend.stock.service.AuditHelper auditHelper;
    @InjectMocks
    private BonCommandeService service;

    private final UUID activiteA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final UUID activiteB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private DemandeAchat da;
    private LigneDemandeAchat ligneDa;
    private Fournisseur fournisseur;
    private final Utilisateur auteur = Utilisateur.builder().id(UUID.randomUUID()).build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tvaTaux", new BigDecimal("0.18"));
        da = DemandeAchat.builder().id(UUID.randomUUID()).codeDossier("DA-2026-0045").statut(DemandeAchat.APPROUVE)
                .entiteEmettrice(Entite.builder().code("UAB_VIE").build()).entiteBeneficiaire(Entite.builder().code("UAB_VIE").build())
                .directionDemandeur(new Structure()).demandeur(auteur).exerciceAnnee(2026)
                .montantEstimeHt(new BigDecimal("2000000")).build();
        ligneDa = LigneDemandeAchat.builder().id(UUID.randomUUID()).demandeAchat(da).description("Laptop").quantite(new BigDecimal("4"))
                .prixUnitaireEstime(new BigDecimal("500000")).activite(BudgetAllocationStructure.builder().id(activiteA).build()).build();
        da.getLignes().add(ligneDa);
        fournisseur = Fournisseur.builder().id(UUID.randomUUID()).codeFournisseur("FOURN-2026-0012")
                .raisonSociale("Afriq Computing").ifu("1234567890A").actif(true).build();

        lenient().when(demandeAchatRepository.findByIdForUpdate(da.getId())).thenReturn(Optional.of(da));
        lenient().when(fournisseurRepository.findById(fournisseur.getId())).thenReturn(Optional.of(fournisseur));
        lenient().when(numeroDocumentService.prochainCodeBonCommande()).thenReturn("BC-2026-0089");
        lenient().when(currentUserService.utilisateurConnecte()).thenReturn(auteur);
        lenient().when(repository.saveAndFlush(any())).thenAnswer(i -> {
            BonCommande b = i.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            return b;
        });
        lenient().when(demandeAchatService.repartitionEngagement(da)).thenReturn(new TreeMap<>(Map.of(activiteA, new BigDecimal("1500000"),
                activiteB, new BigDecimal("500000"))));
    }

    private LigneBCRequestDto ligne(Integer quantite, String prix, String forfait) {
        LigneBCRequestDto l = new LigneBCRequestDto();
        l.setLigneDaId(ligneDa.getId());
        l.setDescription("Laptop i7");
        l.setQuantiteCommandee(quantite == null ? null : BigDecimal.valueOf(quantite));
        l.setPrixUnitaireHt(prix == null ? null : new BigDecimal(prix));
        l.setMontantTotalHt(forfait == null ? null : new BigDecimal(forfait));
        return l;
    }

    private BonCommandeCreateDto requete(LigneBCRequestDto... lignes) {
        BonCommandeCreateDto dto = new BonCommandeCreateDto();
        dto.setDaId(da.getId());
        dto.setFournisseurId(fournisseur.getId());
        dto.setLignes(List.of(lignes));
        return dto;
    }

    @Test
    void creationDepuisUneDaApprouveeCalculeLesMontantsEtPasseLaDaACommande() {
        BonCommandeResponseDto r = service.creer(requete(ligne(4, "500000", null)));

        assertEquals("BC-2026-0089", r.getCodeBc());
        assertEquals("CLASSIQUE", r.getTypeBc());
        assertEquals("EMIS", r.getStatut());
        assertEquals(new BigDecimal("2000000"), r.getMontantTotalHt());
        assertEquals(new BigDecimal("2360000.00"), r.getMontantTotalTtc());
        assertEquals(new BigDecimal("2360000.00"), r.getSoldeRestantTtc());
        assertEquals(DemandeAchat.COMMANDE, da.getStatut());
        verify(budgetApi, never()).liberer(any(), any(), any(), any(), any(), any());
    }

    @Test
    void demandeNonApprouveeRefusee() {
        da.setStatut(DemandeAchat.SOUMIS);
        assertThrows(AchatException.class, () -> service.creer(requete(ligne(4, "500000", null))));
    }

    @Test
    void secondBonPourLaMemeDemandeRefuse() {
        when(repository.existsByDemandeAchatId(da.getId())).thenReturn(true);
        assertThrows(AchatException.class, () -> service.creer(requete(ligne(4, "500000", null))));
    }

    @Test
    void fournisseurSuspenduRefuse() {
        fournisseur.setActif(false);
        assertThrows(AchatException.class, () -> service.creer(requete(ligne(4, "500000", null))));
    }

    @Test
    void montantSuperieurALEstimationRefuse() {
        assertThrows(AchatException.class, () -> service.creer(requete(ligne(4, "500001", null))));
        assertEquals(DemandeAchat.APPROUVE, da.getStatut());
    }

    @Test
    void prixNegocieInferieurLibereLEcartAuProrata() {
        // BC = 1 600 000 sur 2 000 000 estimés : écart 400 000, réparti 75 % / 25 % => 300 000 et 100 000
        service.creer(requete(ligne(4, "400000", null)));

        verify(budgetApi).liberer(eq(activiteA), eq(new BigDecimal("300000.00")), eq("ENGAGEMENT_BC"), eq("BC-2026-0089"), eq(2026), eq(auteur.getId()));
        verify(budgetApi).liberer(eq(activiteB), eq(new BigDecimal("100000.00")), eq("ENGAGEMENT_BC"), eq("BC-2026-0089"), eq(2026), eq(auteur.getId()));
    }

    @Test
    void ligneForfaitaireExigeUnMontantTotal() {
        assertThrows(AchatException.class, () -> service.creer(requete(ligne(null, null, null))));
        BonCommandeResponseDto r = service.creer(requete(ligne(null, null, "1000000")));
        assertEquals(new BigDecimal("1000000"), r.getMontantTotalHt());
        assertNull(r.getLignes().get(0).getQuantiteCommandee());
    }

    @Test
    void quantiteSansPrixRefusee() {
        assertThrows(AchatException.class, () -> service.creer(requete(ligne(4, null, "1000000"))));
    }

    @Test
    void ligneDaEtrangereRefusee() {
        LigneBCRequestDto l = ligne(4, "500000", null);
        l.setLigneDaId(UUID.randomUUID());
        assertThrows(AchatException.class, () -> service.creer(requete(l)));
    }

    @Test
    void typeBcInvalideRefuse() {
        BonCommandeCreateDto dto = requete(ligne(4, "500000", null));
        dto.setTypeBc("BIZARRE");
        assertThrows(AchatException.class, () -> service.creer(dto));
    }

    // --- Imputation des factures (Module 7) ---

    private BonCommande bcEnvoye(String type, String statut) {
        BonCommande bc = BonCommande.builder().id(UUID.randomUUID()).codeBc("BC-2026-0089").typeBc(type).statut(statut)
                .demandeAchat(da).fournisseur(fournisseur).montantTotalHt(new BigDecimal("2000000"))
                .montantTotalTtc(new BigDecimal("2360000")).montantFactureTtc(BigDecimal.ZERO).build();
        lenient().when(repository.findByIdForUpdate(bc.getId())).thenReturn(Optional.of(bc));
        return bc;
    }

    @Test
    void facturePartielleCumuleEtPasseEnPartiellementFacture() {
        BonCommande bc = bcEnvoye("CLASSIQUE", "ENVOYE_FOURNISSEUR");

        BonCommandeResponseDto r = service.imputerFacture(bc.getId(), new BigDecimal("1000000"));

        assertEquals("PARTIELLEMENT_FACTURE", r.getStatut());
        assertEquals(new BigDecimal("1000000"), r.getMontantFactureTtc());
        assertEquals(new BigDecimal("1360000"), r.getSoldeRestantTtc());
    }

    @Test
    void derniereFactureSoldeLeBon() {
        BonCommande bc = bcEnvoye("CLASSIQUE", "PARTIELLEMENT_FACTURE");
        bc.setMontantFactureTtc(new BigDecimal("1000000"));

        BonCommandeResponseDto r = service.imputerFacture(bc.getId(), new BigDecimal("1360000"));

        assertEquals("SOLDE", r.getStatut());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.getSoldeRestantTtc()));
    }

    @Test
    void contratCadreResteEnCoursTantQueNonSolde() {
        BonCommande bc = bcEnvoye("CADRE_ANNUEL", "CADRE_EN_COURS");
        assertEquals("CADRE_EN_COURS", service.imputerFacture(bc.getId(), new BigDecimal("590000")).getStatut());
        assertEquals("SOLDE", service.imputerFacture(bc.getId(), new BigDecimal("1770000")).getStatut());
    }

    @Test
    void factureAuDelaDuSoldeRefuseSansModifierLeBon() {
        BonCommande bc = bcEnvoye("CLASSIQUE", "ENVOYE_FOURNISSEUR");
        assertThrows(AchatException.class, () -> service.imputerFacture(bc.getId(), new BigDecimal("2360000.01")));
        assertEquals(0, BigDecimal.ZERO.compareTo(bc.getMontantFactureTtc()));
        assertEquals("ENVOYE_FOURNISSEUR", bc.getStatut());
    }

    @Test
    void bonNonEnvoyeAnnuleOuSoldeNePeutPasRecevoirDeFacture() {
        for (String statut : List.of("EMIS", "ANNULE", "SOLDE")) {
            BonCommande bc = bcEnvoye("CLASSIQUE", statut);
            assertThrows(AchatException.class, () -> service.imputerFacture(bc.getId(), BigDecimal.TEN), statut);
        }
    }

    @Test
    void montantDeFactureInvalideOuBonInconnuRefuse() {
        BonCommande bc = bcEnvoye("CLASSIQUE", "ENVOYE_FOURNISSEUR");
        assertThrows(AchatException.class, () -> service.imputerFacture(bc.getId(), BigDecimal.ZERO));
        assertThrows(AchatException.class, () -> service.imputerFacture(bc.getId(), null));
        assertThrows(AchatException.class, () -> service.imputerFacture(UUID.randomUUID(), BigDecimal.TEN));
    }

    @Test
    void emissionPasseDeEmisAEnvoyeFournisseurUneSeuleFois() {
        BonCommande bc = BonCommande.builder().id(UUID.randomUUID()).codeBc("BC-1").typeBc("CLASSIQUE").statut("EMIS")
                .demandeAchat(da).fournisseur(fournisseur).montantTotalHt(BigDecimal.TEN).montantTotalTtc(BigDecimal.TEN).build();
        when(repository.findById(bc.getId())).thenReturn(Optional.of(bc));

        assertEquals("ENVOYE_FOURNISSEUR", service.emettre(bc.getId()).getStatut());
        assertThrows(AchatException.class, () -> service.emettre(bc.getId()));
    }

    @Test
    void emissionDUnContratCadreDonneCadreEnCours() {
        BonCommande bc = BonCommande.builder().id(UUID.randomUUID()).codeBc("BC-2").typeBc("CADRE_ANNUEL").statut("EMIS")
                .demandeAchat(da).fournisseur(fournisseur).montantTotalHt(BigDecimal.TEN).montantTotalTtc(BigDecimal.TEN).build();
        when(repository.findById(bc.getId())).thenReturn(Optional.of(bc));
        assertEquals("CADRE_EN_COURS", service.emettre(bc.getId()).getStatut());
    }

    @Test
    void pdfContientLeCodeDuBon() {
        BonCommande bc = BonCommande.builder().id(UUID.randomUUID()).codeBc("BC-2026-0089").typeBc("CLASSIQUE").statut("EMIS")
                .dateEmission(java.time.LocalDate.now()).demandeAchat(da).fournisseur(fournisseur)
                .montantTotalHt(new BigDecimal("2000000")).montantTotalTtc(new BigDecimal("2360000")).build();
        bc.getLignes().add(com.budget.backend.achat.entity.LigneBonCommande.builder().bonCommande(bc).ligneDa(ligneDa)
                .description("Laptop").quantiteCommandee(new BigDecimal("4")).prixUnitaireHt(new BigDecimal("500000"))
                .montantTotalHt(new BigDecimal("2000000")).build());
        when(repository.findById(bc.getId())).thenReturn(Optional.of(bc));

        byte[] pdf = service.genererPdf(bc.getId());

        assertTrue(pdf.length > 500);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }
}
