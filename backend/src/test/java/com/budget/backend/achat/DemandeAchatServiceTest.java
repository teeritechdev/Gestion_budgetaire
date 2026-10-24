package com.budget.backend.achat;

import com.budget.backend.achat.api.BudgetReservationApi;
import com.budget.backend.workflow.api.WorkflowEngineApi;
import com.budget.backend.workflow.api.WorkflowEngineApi.EtatWorkflow;
import com.budget.backend.workflow.api.WorkflowTransitionEvent;
import com.budget.backend.achat.dto.*;
import com.budget.backend.achat.entity.DemandeAchat;
import com.budget.backend.achat.entity.HistoriqueValidation;
import com.budget.backend.achat.repository.DemandeAchatRepository;
import com.budget.backend.achat.service.DemandeAchatService;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.EntiteRepository;
import com.budget.backend.security.repository.StructureRepository;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.NumeroDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandeAchatServiceTest {

    @Mock
    private DemandeAchatRepository repository;
    @Mock
    private EntiteRepository entiteRepository;
    @Mock
    private StructureRepository structureRepository;
    @Mock
    private BudgetAllocationStructureRepository allocationRepository;
    @Mock
    private com.budget.backend.stock.repository.ArticleRepository articleRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private NumeroDocumentService numeroDocumentService;
    @Mock
    private BudgetReservationApi budgetApi;
    @Mock
    private WorkflowEngineApi workflowApi;
    @Mock
    private com.budget.backend.stock.service.AuditHelper auditHelper;
    @InjectMocks
    private DemandeAchatService service;

    private final UUID ligneA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final UUID ligneB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    // Ligne appartenant à une autre entité (UAB_IARDT), pour les ventilations inter-filiales
    private final UUID ligneC = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    // Ligne d'un exercice différent et ligne d'une autre direction de la même entité
    private final UUID ligneAnciennee = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private final UUID ligneAutreDirection = UUID.fromString("00000000-0000-0000-0000-0000000000d2");
    private final UUID ligneDansService = UUID.fromString("00000000-0000-0000-0000-0000000000d3");
    private Structure direction;
    private Utilisateur demandeur;
    private Utilisateur valideur;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tvaTaux", new BigDecimal("0.18"));
        demandeur = Utilisateur.builder().id(UUID.randomUUID()).build();
        valideur = Utilisateur.builder().id(UUID.randomUUID()).build();

        lenient().when(currentUserService.utilisateurConnecte()).thenReturn(demandeur);
        lenient().when(numeroDocumentService.prochainCodeDemandeAchat()).thenReturn("DA-2026-0001");
        lenient().when(entiteRepository.findById(anyString())).thenAnswer(i -> Optional.of(Entite.builder().code(i.getArgument(0)).build()));
        direction = structure("DIR-IT", Entite.builder().code("UAB_VIE").build(), null, null);
        lenient().when(structureRepository.findById(any())).thenReturn(Optional.of(direction));
        lenient().when(allocationRepository.findById(ligneA)).thenReturn(Optional.of(allocation(ligneA, 2026, direction)));
        lenient().when(allocationRepository.findById(ligneB)).thenReturn(Optional.of(allocation(ligneB, 2026, direction)));
        lenient().when(allocationRepository.findById(ligneC)).thenReturn(Optional.of(allocation(ligneC, 2026,
                structure("DIR-IARDT", Entite.builder().code("UAB_IARDT").build(), null, null))));
        lenient().when(allocationRepository.findById(ligneAnciennee)).thenReturn(Optional.of(allocation(ligneAnciennee, 2025, direction)));
        Structure autreDirection = structure("DIR-RH", Entite.builder().code("UAB_VIE").build(), null, null);
        lenient().when(allocationRepository.findById(ligneAutreDirection))
                .thenReturn(Optional.of(allocation(ligneAutreDirection, 2026, autreDirection)));
        // un service rattaché à la direction (par parent), accepté ; un autre rattaché par le champ direction
        Structure service = structure("SERV-RESEAU", Entite.builder().code("UAB_VIE").build(), direction, null);
        lenient().when(allocationRepository.findById(ligneDansService)).thenReturn(Optional.of(allocation(ligneDansService, 2026, service)));
        lenient().when(repository.saveAndFlush(any())).thenAnswer(i -> {
            DemandeAchat d = i.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            return d;
        });
        lenient().when(workflowApi.demarrer(any(), any(), any())).thenReturn(new EtatWorkflow("VALIDATION_N1", "EN_ATTENTE_VALIDATION"));
    }

    private Structure structure(String code, Entite entite, Structure parent, Structure directionDeRattachement) {
        Structure s = new Structure();
        s.setId(UUID.randomUUID());
        s.setCode(code);
        s.setEntite(entite);
        s.setParent(parent);
        s.setDirection(directionDeRattachement);
        return s;
    }

    private BudgetAllocationStructure allocation(UUID id, int annee, Structure proprietaire) {
        var exercice = new com.budget.backend.budget.entity.ExerciceBudgetaire();
        exercice.setAnnee(annee);
        var os = new com.budget.backend.budget.entity.BudgetAllocationOs();
        os.setExerciceBudgetaire(exercice);
        var op = new com.budget.backend.budget.entity.BudgetAllocationOp();
        op.setBudgetAllocationOs(os);
        var act = new com.budget.backend.budget.entity.BudgetAllocationActivite();
        act.setBudgetAllocationOp(op);
        return BudgetAllocationStructure.builder().id(id).budgetAllocationActivite(act).structure(proprietaire).build();
    }

    private LigneDARequestDto ligne(UUID activite, int quantite, String prix) {
        LigneDARequestDto l = new LigneDARequestDto();
        l.setActiviteId(activite);
        l.setDescription("Ordinateur");
        l.setUniteMesure("UNITE");
        l.setQuantite(BigDecimal.valueOf(quantite));
        l.setPrixUnitaireEstime(new BigDecimal(prix));
        return l;
    }

    private DemandeAchatRequestDto requete(List<LigneDARequestDto> lignes, List<DossierVentilationRequestDto> ventilations) {
        DemandeAchatRequestDto dto = new DemandeAchatRequestDto();
        dto.setEntiteEmettrice("UAB_VIE");
        dto.setEntiteBeneficiaire("UAB_VIE");
        dto.setTypeAchat("INVESTISSEMENT_IMMO");
        dto.setObjet("Renouvellement");
        dto.setJustification("Besoin");
        dto.setDirectionDemandeurId(direction.getId());
        dto.setExerciceAnnee(2026);
        dto.setLignes(lignes);
        dto.setVentilations(ventilations);
        return dto;
    }

    private DossierVentilationRequestDto ventilation(String entite, UUID activite, String montant, String pourcentage) {
        DossierVentilationRequestDto v = new DossierVentilationRequestDto();
        v.setEntiteImpactee(entite);
        v.setActiviteImpacteeId(activite);
        v.setMontantImpute(new BigDecimal(montant));
        v.setPourcentageApplique(new BigDecimal(pourcentage));
        return v;
    }

    @Test
    void creationCalculeHtTtcEtEngageParLigneBudgetaire() {
        // 4 x 500 000 = 2 000 000 HT sur A ; 1 x 300 000 sur B => 2 300 000 HT ; TTC = x 1,18 = 2 714 000
        DemandeAchatResponseDto r = service.creer(requete(List.of(ligne(ligneA, 4, "500000"), ligne(ligneB, 1, "300000")), null));

        assertEquals(new BigDecimal("2300000"), r.getMontantEstimeHt());
        assertEquals(new BigDecimal("2714000.00"), r.getMontantEstimeTtc());
        assertEquals("DA-2026-0001", r.getCodeDossier());
        assertEquals("SOUMIS", r.getStatut());
        assertEquals("VALIDATION_N1", r.getEtapeCourante());
        verify(budgetApi).engager(eq(ligneA), eq(new BigDecimal("2000000")), eq("ENGAGEMENT_DA"), eq("DA-2026-0001"), eq(2026), eq(demandeur.getId()));
        verify(budgetApi).engager(eq(ligneB), eq(new BigDecimal("300000")), eq("ENGAGEMENT_DA"), eq("DA-2026-0001"), eq(2026), eq(demandeur.getId()));
    }

    @Test
    void ventilationInterFilialesEngageSelonLesImputations() {
        // 2 000 000 HT : 75 % sur A (1 500 000) et 25 % sur B (500 000)
        service.creer(requete(List.of(ligne(ligneA, 4, "500000")),
                List.of(ventilation("UAB_VIE", ligneA, "1500000", "75"), ventilation("UAB_IARDT", ligneC, "500000", "25"))));

        verify(budgetApi).engager(eq(ligneA), eq(new BigDecimal("1500000")), any(), any(), any(), any());
        verify(budgetApi).engager(eq(ligneC), eq(new BigDecimal("500000")), any(), any(), any(), any());
    }

    @Test
    void ventilationQuiNeTotalisePasCentPourCentEstRefusee() {
        assertThrows(AchatException.class, () -> service.creer(requete(List.of(ligne(ligneA, 4, "500000")),
                List.of(ventilation("UAB_VIE", ligneA, "1500000", "75"), ventilation("UAB_IARDT", ligneC, "500000", "20")))));
        verify(budgetApi, never()).engager(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ventilationDontLesMontantsNeCorrespondentPasAuHtEstRefusee() {
        assertThrows(AchatException.class, () -> service.creer(requete(List.of(ligne(ligneA, 4, "500000")),
                List.of(ventilation("UAB_VIE", ligneA, "1000000", "75"), ventilation("UAB_IARDT", ligneC, "500000", "25")))));
    }

    // --- Cohérence budgétaire ---

    @Test
    void ligneBudgetaireDUnAutreExerciceRefusee() {
        AchatException e = assertThrows(AchatException.class,
                () -> service.creer(requete(List.of(ligne(ligneAnciennee, 1, "100")), null)));
        assertTrue(e.getMessage().contains("2025"));
        verify(budgetApi, never()).engager(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ligneBudgetaireDUneAutreDirectionRefusee() {
        assertThrows(AchatException.class, () -> service.creer(requete(List.of(ligne(ligneAutreDirection, 1, "100")), null)));
        verify(budgetApi, never()).engager(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ligneDUnServiceRattacheALaDirectionAcceptee() {
        service.creer(requete(List.of(ligne(ligneDansService, 1, "100")), null));
        verify(budgetApi).engager(eq(ligneDansService), eq(new BigDecimal("100")), any(), any(), any(), any());
    }

    @Test
    void ventilationVersUneEntiteQuiNePossedePasLaLigneRefusee() {
        // ligneA appartient à UAB_VIE, pas à UAB_IARDT
        assertThrows(AchatException.class, () -> service.creer(requete(List.of(ligne(ligneA, 4, "500000")),
                List.of(ventilation("UAB_VIE", ligneB, "1500000", "75"), ventilation("UAB_IARDT", ligneA, "500000", "25")))));
    }

    @Test
    void ventilationSurUnAutreExerciceRefusee() {
        assertThrows(AchatException.class, () -> service.creer(requete(List.of(ligne(ligneA, 4, "500000")),
                List.of(ventilation("UAB_VIE", ligneAnciennee, "2000000", "100")))));
    }

    @Test
    void creationEtApprobationSontTraceesDansLAudit() {
        service.creer(requete(List.of(ligne(ligneA, 1, "100")), null));
        verify(auditHelper).tracer(eq(demandeur), eq("INSERTION"), eq("demandes_achat"), any(), any(), any());
    }

    @Test
    void budgetInsuffisantFaitEchouerLaCreation() {
        doThrow(new AchatException("Budget insuffisant")).when(budgetApi).engager(any(), any(), any(), any(), any(), any());
        assertThrows(AchatException.class, () -> service.creer(requete(List.of(ligne(ligneA, 4, "500000")), null)));
        verify(workflowApi, never()).demarrer(any(), any(), any());
    }

    @Test
    void codeDossierDejaUtiliseEstRefuse() {
        DemandeAchatRequestDto dto = requete(List.of(ligne(ligneA, 1, "10")), null);
        dto.setCodeDossier("DA-2026-0099");
        when(repository.existsByCodeDossier("DA-2026-0099")).thenReturn(true);
        assertThrows(AchatException.class, () -> service.creer(dto));
    }

    // --- Validation ---

    private DemandeAchat demandeExistante(String statut, String etape) {
        DemandeAchat da = DemandeAchat.builder().id(UUID.randomUUID()).codeDossier("DA-2026-0001")
                .entiteEmettrice(Entite.builder().code("UAB_VIE").build())
                .entiteBeneficiaire(Entite.builder().code("UAB_VIE").build())
                .directionDemandeur(new Structure()).demandeur(demandeur)
                .statut(statut).etapeCourante(etape).exerciceAnnee(2026)
                .montantEstimeHt(new BigDecimal("2000000")).build();
        da.getLignes().add(com.budget.backend.achat.entity.LigneDemandeAchat.builder().demandeAchat(da)
                .activite(BudgetAllocationStructure.builder().id(ligneA).build())
                .quantite(new BigDecimal("4")).prixUnitaireEstime(new BigDecimal("500000")).build());
        lenient().when(repository.findByIdForUpdate(da.getId())).thenReturn(Optional.of(da));
        return da;
    }

    private WorkflowExecutionActionDto action(String a) {
        WorkflowExecutionActionDto dto = new WorkflowExecutionActionDto();
        dto.setAction(a);
        dto.setCommentaire("ok");
        return dto;
    }

    @Test
    void approbationIntermediaireMetLeDossierEnCoursDeValidation() {
        DemandeAchat da = demandeExistante("SOUMIS", "VALIDATION_N1");
        when(currentUserService.utilisateurConnecte()).thenReturn(valideur);
        when(workflowApi.prochaineEtape(any(), any(), any(), eq(true), eq(valideur)))
                .thenReturn(new EtatWorkflow("CONTROLE_BUDGETAIRE", "EN_ATTENTE_VALIDATION"));

        DemandeAchatResponseDto r = service.approuver(da.getId(), action("approve"));

        assertEquals("EN_COURS_VALIDATION", r.getStatut());
        assertEquals("CONTROLE_BUDGETAIRE", r.getEtapeCourante());
        assertEquals(1, r.getHistorique().size());
        assertEquals("VALIDATION_N1", r.getHistorique().get(0).getEtape());
        assertEquals(HistoriqueValidation.VALIDE, r.getHistorique().get(0).getDecision());
        verify(budgetApi, never()).liberer(any(), any(), any(), any(), any(), any());
    }

    @Test
    void approbationFinaleDonneLeStatutApprouve() {
        DemandeAchat da = demandeExistante("EN_COURS_VALIDATION", "VALIDATION_DG");
        when(currentUserService.utilisateurConnecte()).thenReturn(valideur);
        when(workflowApi.prochaineEtape(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new EtatWorkflow(null, "APPROUVE")); // le moteur ne renvoie plus d'étape en fin de circuit

        DemandeAchatResponseDto r = service.approuver(da.getId(), action("APPROVE"));

        assertEquals("APPROUVE", r.getStatut());
        assertEquals("TRAITEMENT_CSP", r.getEtapeCourante());
    }

    @Test
    void rejetLiberLesCreditsEngages() {
        DemandeAchat da = demandeExistante("SOUMIS", "VALIDATION_N1");
        when(currentUserService.utilisateurConnecte()).thenReturn(valideur);
        when(workflowApi.prochaineEtape(any(), any(), any(), eq(false), any())).thenReturn(new EtatWorkflow(null, "REJETE"));

        DemandeAchatResponseDto r = service.approuver(da.getId(), action("REJECT"));

        assertEquals("REJETE", r.getStatut());
        assertEquals(HistoriqueValidation.REJETE, r.getHistorique().get(0).getDecision());
        verify(budgetApi).liberer(eq(ligneA), eq(new BigDecimal("2000000")), eq("ENGAGEMENT_DA"), eq("DA-2026-0001"), eq(2026), eq(valideur.getId()));
    }

    @Test
    void demandeurNePeutPasValiderSaPropreDemande() {
        DemandeAchat da = demandeExistante("SOUMIS", "VALIDATION_N1");
        assertThrows(AchatException.class, () -> service.approuver(da.getId(), action("APPROVE")));
        verify(workflowApi, never()).prochaineEtape(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void demandeDejaTermineeOuActionInvalideRefusees() {
        DemandeAchat approuvee = demandeExistante("APPROUVE", "TRAITEMENT_CSP");
        assertThrows(AchatException.class, () -> service.approuver(approuvee.getId(), action("APPROVE")));

        DemandeAchat soumise = demandeExistante("SOUMIS", "VALIDATION_N1");
        assertThrows(AchatException.class, () -> service.approuver(soumise.getId(), action("PEUTETRE")));
    }

    @Test
    void rejetVersUneEtapeDExceptionGardeLesCreditsEngagesJusquALaFin() {
        // REJETE mais une étape d'annulation reste à traiter : la DA reste en cours, rien n'est libéré
        DemandeAchat da = demandeExistante("EN_COURS_VALIDATION", "VALIDATION_DG");
        when(currentUserService.utilisateurConnecte()).thenReturn(valideur);
        when(workflowApi.prochaineEtape(any(), any(), any(), eq(false), any())).thenReturn(new EtatWorkflow("ANNULATION", "REJETE"));

        DemandeAchatResponseDto r = service.approuver(da.getId(), action("REJECT"));

        assertEquals("EN_COURS_VALIDATION", r.getStatut());
        assertEquals("ANNULATION", r.getEtapeCourante());
        verify(budgetApi, never()).liberer(any(), any(), any(), any(), any(), any());
    }

    @Test
    void finDeCircuitAnnuleLibereLesCreditsEtRejetteLaDemande() {
        DemandeAchat da = demandeExistante("EN_COURS_VALIDATION", "ANNULATION");
        when(currentUserService.utilisateurConnecte()).thenReturn(valideur);
        when(workflowApi.prochaineEtape(any(), any(), any(), eq(true), any())).thenReturn(new EtatWorkflow(null, "ANNULE"));

        DemandeAchatResponseDto r = service.approuver(da.getId(), action("APPROVE"));

        assertEquals("REJETE", r.getStatut());
        verify(budgetApi).liberer(eq(ligneA), eq(new BigDecimal("2000000")), eq("ENGAGEMENT_DA"), any(), any(), any());
    }

    // --- Décision prise directement sur le moteur (événement) ---

    @Test
    void decisionViaLeMoteurApplique_lesMemesEffetsQueApprouver() {
        DemandeAchat da = demandeExistante("SOUMIS", "VALIDATION_N1");

        service.surDecisionWorkflow(new WorkflowTransitionEvent("WORKFLOW_DA", da.getId(), "VALIDATION_N1", true,
                new EtatWorkflow("CONTROLE_BUDGETAIRE", "EN_ATTENTE_VALIDATION"), da.getMontantEstimeHt(), valideur, "ok"));

        assertEquals("EN_COURS_VALIDATION", da.getStatut());
        assertEquals("CONTROLE_BUDGETAIRE", da.getEtapeCourante());
        assertEquals(1, da.getHistorique().size());
        assertEquals("VALIDATION_N1", da.getHistorique().get(0).getEtape());
    }

    @Test
    void rejetViaLeMoteurLibereLesCredits() {
        DemandeAchat da = demandeExistante("SOUMIS", "VALIDATION_N1");

        service.surDecisionWorkflow(new WorkflowTransitionEvent("WORKFLOW_DA", da.getId(), "VALIDATION_N1", false,
                new EtatWorkflow(null, "REJETE"), da.getMontantEstimeHt(), valideur, "non"));

        assertEquals("REJETE", da.getStatut());
        verify(budgetApi).liberer(eq(ligneA), eq(new BigDecimal("2000000")), any(), any(), any(), any());
    }

    @Test
    void decisionViaLeMoteurRefuseeSiLeDemandeurValideSaPropreDemande() {
        DemandeAchat da = demandeExistante("SOUMIS", "VALIDATION_N1");
        assertThrows(com.budget.backend.workflow.WorkflowException.class, () -> service.surDecisionWorkflow(new WorkflowTransitionEvent("WORKFLOW_DA", da.getId(),
                "VALIDATION_N1", true, new EtatWorkflow("CONTROLE_BUDGETAIRE", "EN_ATTENTE_VALIDATION"), BigDecimal.TEN, demandeur, null)));
        assertEquals("SOUMIS", da.getStatut());
    }

    @Test
    void evenementDUnAutreWorkflowEstIgnore() {
        DemandeAchat da = demandeExistante("SOUMIS", "VALIDATION_N1");
        service.surDecisionWorkflow(new WorkflowTransitionEvent("WORKFLOW_PV", da.getId(), "X", true,
                new EtatWorkflow(null, "APPROUVE"), BigDecimal.TEN, valideur, null));
        assertEquals("SOUMIS", da.getStatut());
        verify(repository, never()).findByIdForUpdate(any());
    }

    // --- Modification ---

    @Test
    void modificationRefuseeUneFoisLaValidationCommencee() {
        DemandeAchat da = demandeExistante("EN_COURS_VALIDATION", "CONTROLE_BUDGETAIRE");
        assertThrows(AchatException.class, () -> service.modifier(da.getId(), requete(List.of(ligne(ligneA, 1, "10")), null)));
    }

    @Test
    void modificationRefuseeSiCEstUnAutreUtilisateur() {
        DemandeAchat da = demandeExistante("SOUMIS", "VALIDATION_N1");
        when(currentUserService.utilisateurConnecte()).thenReturn(valideur);
        assertThrows(AchatException.class, () -> service.modifier(da.getId(), requete(List.of(ligne(ligneA, 1, "10")), null)));
    }

    @Test
    void modificationLibereAncienEngagementPuisEngageLeNouveau() {
        DemandeAchat da = demandeExistante("SOUMIS", "VALIDATION_N1");

        DemandeAchatResponseDto r = service.modifier(da.getId(), requete(List.of(ligne(ligneB, 2, "100000")), null));

        assertEquals(new BigDecimal("200000"), r.getMontantEstimeHt());
        verify(budgetApi).liberer(eq(ligneA), eq(new BigDecimal("2000000")), any(), any(), any(), any());
        verify(budgetApi).engager(eq(ligneB), eq(new BigDecimal("200000")), any(), any(), any(), any());
    }
}
