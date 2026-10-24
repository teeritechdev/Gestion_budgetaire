package com.budget.backend.workflow;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.entity.Role;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.stock.service.AuditHelper;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.workflow.api.DossierMontantProvider;
import com.budget.backend.workflow.api.WorkflowEngineApi.EtatWorkflow;
import com.budget.backend.workflow.api.WorkflowTransitionEvent;
import com.budget.backend.workflow.dto.WorkflowExecutionActionDto;
import com.budget.backend.workflow.dto.WorkflowInstanceResponseDto;
import com.budget.backend.workflow.entity.WorkflowDefinition;
import com.budget.backend.workflow.entity.WorkflowEtape;
import com.budget.backend.workflow.entity.WorkflowInstance;
import com.budget.backend.workflow.repository.WorkflowDefinitionRepository;
import com.budget.backend.workflow.repository.WorkflowInstanceRepository;
import com.budget.backend.workflow.service.WorkflowEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Teste le moteur sur le scénario du PDF : « Achat de serveurs informatiques (12 000 000 FCFA) ». */
@ExtendWith(MockitoExtension.class)
class WorkflowEngineServiceTest {

    @Mock
    private WorkflowDefinitionRepository definitionRepository;
    @Mock
    private WorkflowInstanceRepository instanceRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AuditHelper auditHelper;
    @InjectMocks
    private WorkflowEngineService service;

    private WorkflowDefinition definition;
    private WorkflowEtape etp009; // Validation DFI
    private WorkflowEtape etp010b; // Approbation DG, à partir de 50 000,01
    private WorkflowEtape etp011; // Circuit d'exception (annulation / libération des crédits)
    private WorkflowEtape etp012; // Etablissement du contrat
    private final UUID dossier = UUID.randomUUID();
    private final Map<UUID, WorkflowInstance> base = new HashMap<>();

    @BeforeEach
    void setUp() {
        definition = WorkflowDefinition.builder().id(UUID.randomUUID()).code("CHAINE_ENGAGEMENT").libelle("Chaîne d'engagement UAB").actif(true).build();
        etp009 = etape(9, "VAL_DFI", "ENGAGEMENT_VALIDER_DFI", "0.00", null);
        etp010b = etape(10, "APP_DG", "ENGAGEMENT_APPROUVER_DG", "50000.01", null);
        etp011 = etape(11, "EXCEPTION_CG", "ENGAGEMENT_ANNULER_CG", null, null);
        etp012 = etape(12, "CONTRAT_CSAMG", "CONTRAT_ETABLIR", null, null);
        etp009.setEtapeSuivanteSucces(etp010b);
        etp010b.setEtapeSuivanteSucces(etp012);
        etp010b.setEtapeSuivanteRejet(etp011);

        lenient().when(definitionRepository.findByCode("CHAINE_ENGAGEMENT")).thenReturn(Optional.of(definition));
        lenient().when(instanceRepository.saveAndFlush(any())).thenAnswer(i -> {
            WorkflowInstance w = i.getArgument(0);
            if (w.getId() == null) w.setId(UUID.randomUUID());
            base.put(w.getDossierId(), w);
            return w;
        });
        lenient().when(instanceRepository.findForUpdate(eq("CHAINE_ENGAGEMENT"), any()))
                .thenAnswer(i -> Optional.ofNullable(base.get(i.<UUID>getArgument(1))));
    }

    private WorkflowEtape etape(int num, String code, String permission, String min, String max) {
        WorkflowEtape e = WorkflowEtape.builder().id(UUID.randomUUID()).workflow(definition).numEtape(num).codeEtape(code)
                .libelle(code).permissionRequise(permission)
                .montantMinimum(min == null ? null : new BigDecimal(min)).montantMaximum(max == null ? null : new BigDecimal(max)).build();
        definition.getEtapes().add(e);
        return e;
    }

    private Utilisateur utilisateurAvec(String... permissions) {
        Role role = Role.builder().code("R").permissions(new HashSet<>()).build();
        Arrays.stream(permissions).forEach(p -> role.getPermissions().add(Permission.builder().code(p).build()));
        return Utilisateur.builder().id(UUID.randomUUID()).role(role).build();
    }

    private final Utilisateur dfi = utilisateurAvec("ENGAGEMENT_VALIDER_DFI");
    private final Utilisateur dg = utilisateurAvec("ENGAGEMENT_APPROUVER_DG");

    private static BigDecimal m(String v) {
        return new BigDecimal(v);
    }

    // --- Démarrage ---

    @Test
    void ledossierArriveALEtape9ValidationDfi() {
        EtatWorkflow etat = service.demarrer("CHAINE_ENGAGEMENT", dossier, m("12000000"));

        assertEquals("VAL_DFI", etat.etapeCourante());
        assertEquals("EN_ATTENTE_VALIDATION", etat.statut());
        assertEquals(etp009, base.get(dossier).getEtapeCourante());
    }

    @Test
    void demarrageRefuseSiWorkflowInconnuInactifOuDejaDemarre() {
        assertThrows(WorkflowException.class, () -> service.demarrer("INCONNU", dossier, m("1")));

        definition.setActif(false);
        assertThrows(WorkflowException.class, () -> service.demarrer("CHAINE_ENGAGEMENT", dossier, m("1")));

        definition.setActif(true);
        when(instanceRepository.existsByWorkflowCodeAndDossierId("CHAINE_ENGAGEMENT", dossier)).thenReturn(true);
        assertThrows(WorkflowException.class, () -> service.demarrer("CHAINE_ENGAGEMENT", dossier, m("1")));
    }

    @Test
    void demarrageRefuseSiAucuneEtapeNeCouvreLeMontant() {
        definition.getEtapes().clear();
        etape(1, "SEULE", "P", "100.00", "200.00");
        assertThrows(WorkflowException.class, () -> service.demarrer("CHAINE_ENGAGEMENT", dossier, m("50")));
    }

    // --- Circuit nominal : le scénario du PDF ---

    @Test
    void scenarioDuPdfDouzeMillionsParcourtDfiDgPuisContrat() {
        service.demarrer("CHAINE_ENGAGEMENT", dossier, m("12000000"));

        // Phase 3 : le DFI valide ; 12 000 000 > 50 000 donc étape suivante = ETP-010B (Approbation DG)
        EtatWorkflow e1 = service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), true, dfi);
        assertEquals("APP_DG", e1.etapeCourante());
        assertEquals("EN_ATTENTE_VALIDATION", e1.statut());

        // Phase 4, cas A : le DG valide -> ETP-012 (établissement du contrat)
        EtatWorkflow e2 = service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), true, dg);
        assertEquals("CONTRAT_CSAMG", e2.etapeCourante());

        // Dernière étape sans suite : circuit approuvé
        EtatWorkflow e3 = service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), true, utilisateurAvec("CONTRAT_ETABLIR"));
        assertEquals("APPROUVE", e3.statut());
        assertTrue(e3.termine());
    }

    @Test
    void phase4CasBLeDgRefuseVersLeCircuitDException() {
        service.demarrer("CHAINE_ENGAGEMENT", dossier, m("12000000"));
        service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), true, dfi);

        EtatWorkflow rejet = service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), false, dg);
        assertEquals("EXCEPTION_CG", rejet.etapeCourante());   // ETP-011
        assertEquals("REJETE", rejet.statut());
        assertFalse(rejet.termine());

        // L'agent de contrôle de gestion traite l'annulation : fin du circuit en ANNULE
        EtatWorkflow fin = service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), true, utilisateurAvec("ENGAGEMENT_ANNULER_CG"));
        assertEquals("ANNULE", fin.statut());
        assertTrue(fin.termine());
    }

    @Test
    void rejetSansEtapeDeRejetTermineLeCircuitEnRejete() {
        service.demarrer("CHAINE_ENGAGEMENT", dossier, m("12000000"));

        EtatWorkflow e = service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), false, dfi);

        assertEquals("REJETE", e.statut());
        assertTrue(e.termine());
    }

    // --- Plages de montants ---

    @Test
    void sousLeSeuilLEtapeDgEstSautee() {
        service.demarrer("CHAINE_ENGAGEMENT", dossier, m("40000"));

        EtatWorkflow e = service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("40000"), true, dfi);

        assertEquals("CONTRAT_CSAMG", e.etapeCourante()); // DG sautée, on suit son renvoi de succès
    }

    @Test
    void bornesDeLaPlage_50000ExcluEtCinquanteMilleVirguleZeroUnInclus() {
        assertFalse(etp010b.estApplicable(m("50000")));
        assertFalse(etp010b.estApplicable(m("50000.00")));
        assertTrue(etp010b.estApplicable(m("50000.01")));
        assertTrue(etp010b.estApplicable(m("999999999")));
        assertTrue(etp009.estApplicable(BigDecimal.ZERO));

        WorkflowEtape bornee = etape(20, "BORNEE", "P", "100", "200");
        assertTrue(bornee.estApplicable(m("100")));
        assertTrue(bornee.estApplicable(m("200")));
        assertFalse(bornee.estApplicable(m("200.01")));
        assertFalse(bornee.estApplicable(m("99.99")));
        assertTrue(etp009.estApplicable(null)); // montant absent traité comme 0
    }

    @Test
    void uneBoucleDeRenvoisEstDetectee() {
        WorkflowEtape a = etape(30, "A", "P", "1000", null);
        WorkflowEtape b = etape(31, "B", "P", "1000", null);
        a.setEtapeSuivanteSucces(b);
        b.setEtapeSuivanteSucces(a);
        etp009.setEtapeSuivanteSucces(a);
        service.demarrer("CHAINE_ENGAGEMENT", dossier, m("10")); // A et B ne s'appliquent pas à 10

        assertThrows(WorkflowException.class, () -> service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("10"), true, dfi));
    }

    // --- Permissions et erreurs ---

    @Test
    void permissionDeLEtapeObligatoire_etInsensibleALaCasse() {
        service.demarrer("CHAINE_ENGAGEMENT", dossier, m("12000000"));

        assertThrows(WorkflowException.class, () -> service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), true, dg));
        assertThrows(WorkflowException.class, () -> service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), true, Utilisateur.builder().build()));
        assertEquals("VAL_DFI", base.get(dossier).getEtapeCourante().getCodeEtape()); // rien n'a bougé

        EtatWorkflow e = service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), true, utilisateurAvec("engagement_valider_dfi"));
        assertEquals("APP_DG", e.etapeCourante());
    }

    @Test
    void circuitTermineOuDossierInconnuRefuses() {
        assertThrows(WorkflowException.class, () -> service.prochaineEtape("CHAINE_ENGAGEMENT", UUID.randomUUID(), m("1"), true, dfi));

        service.demarrer("CHAINE_ENGAGEMENT", dossier, m("12000000"));
        service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), false, dfi); // rejet sans suite : terminé
        assertThrows(WorkflowException.class, () -> service.prochaineEtape("CHAINE_ENGAGEMENT", dossier, m("12000000"), true, dfi));
    }

    // --- API REST : décision directe sur le moteur ---

    private WorkflowExecutionActionDto action(String a) {
        WorkflowExecutionActionDto dto = new WorkflowExecutionActionDto();
        dto.setDossierId(dossier);
        dto.setAction(a);
        dto.setCommentaire("ok");
        return dto;
    }

    @Test
    void executerUtiliseLeMontantDuModuleProprietaireEtPublieUnEvenement() {
        DossierMontantProvider provider = new DossierMontantProvider() {
            public boolean supporte(String code) { return "CHAINE_ENGAGEMENT".equals(code); }
            public Optional<BigDecimal> montant(UUID id) { return Optional.of(m("12000000")); }
        };
        ReflectionTestUtils.setField(service, "montantProviders", List.of(provider));
        service.demarrer("CHAINE_ENGAGEMENT", dossier, m("12000000"));
        when(instanceRepository.findByDossierIdOrderByCreatedAtDesc(dossier)).thenReturn(List.of(base.get(dossier)));
        when(currentUserService.utilisateurConnecte()).thenReturn(dfi);

        WorkflowInstanceResponseDto r = service.executer(action("approve"));

        assertEquals("EN_ATTENTE_VALIDATION", r.getStatutActuel());
        assertEquals("APP_DG", r.getEtapeCourante().getCodeEtape());
        ArgumentCaptor<WorkflowTransitionEvent> captor = ArgumentCaptor.forClass(WorkflowTransitionEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        WorkflowTransitionEvent ev = captor.getValue();
        assertEquals("CHAINE_ENGAGEMENT", ev.workflowCode());
        assertEquals("VAL_DFI", ev.etapeAvant());
        assertTrue(ev.approuve());
        assertEquals("APP_DG", ev.etatApres().etapeCourante());
        assertEquals(m("12000000"), ev.montant());
        verify(auditHelper).tracer(any(), eq("VALIDATION"), eq("workflow_instances"), any(), any(), any());
    }

    @Test
    void executerRefuseActionInvalideCircuitTermineOuMontantIntrouvable() {
        when(currentUserService.utilisateurConnecte()).thenReturn(dfi);
        assertThrows(WorkflowException.class, () -> service.executer(action("PEUTETRE")));

        when(instanceRepository.findByDossierIdOrderByCreatedAtDesc(dossier)).thenReturn(List.of());
        assertThrows(WorkflowException.class, () -> service.executer(action("APPROVE")));

        service.demarrer("CHAINE_ENGAGEMENT", dossier, m("1"));
        when(instanceRepository.findByDossierIdOrderByCreatedAtDesc(dossier)).thenReturn(List.of(base.get(dossier)));
        // aucun fournisseur de montant : le moteur ne peut pas décider
        assertThrows(WorkflowException.class, () -> service.executer(action("APPROVE")));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
