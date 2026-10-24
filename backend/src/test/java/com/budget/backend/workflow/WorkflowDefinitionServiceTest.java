package com.budget.backend.workflow;

import com.budget.backend.workflow.dto.WorkflowDefinitionRequestDto;
import com.budget.backend.workflow.dto.WorkflowDefinitionResponseDto;
import com.budget.backend.workflow.dto.WorkflowEtapeRequestDto;
import com.budget.backend.workflow.dto.WorkflowEtapeResponseDto;
import com.budget.backend.workflow.entity.WorkflowDefinition;
import com.budget.backend.workflow.repository.WorkflowDefinitionRepository;
import com.budget.backend.workflow.repository.WorkflowEtapeRepository;
import com.budget.backend.workflow.service.WorkflowDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowDefinitionServiceTest {

    @Mock
    private WorkflowDefinitionRepository repository;
    @Mock
    private WorkflowEtapeRepository etapeRepository;
    @InjectMocks
    private WorkflowDefinitionService service;

    @BeforeEach
    void setUp() {
        // simule la génération des identifiants à l'enregistrement
        lenient().when(repository.saveAndFlush(any())).thenAnswer(i -> {
            WorkflowDefinition d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            d.getEtapes().forEach(e -> e.setId(UUID.randomUUID()));
            return d;
        });
    }

    private WorkflowEtapeRequestDto etape(int num, String code, String permission, String min, String succes, String rejet) {
        WorkflowEtapeRequestDto e = new WorkflowEtapeRequestDto();
        e.setNumEtape(num);
        e.setCodeEtape(code);
        e.setLibelle(code);
        e.setPermissionRequise(permission);
        e.setMontantMinimum(min == null ? null : new BigDecimal(min));
        e.setEtapeSuivanteSuccesCode(succes);
        e.setEtapeSuivanteRejetCode(rejet);
        return e;
    }

    private WorkflowDefinitionRequestDto requete(WorkflowEtapeRequestDto... etapes) {
        WorkflowDefinitionRequestDto dto = new WorkflowDefinitionRequestDto();
        dto.setCode("CHAINE_ENGAGEMENT");
        dto.setLibelle("Chaîne d'engagement UAB");
        dto.setEtapes(List.of(etapes));
        return dto;
    }

    @Test
    void creationDuScenarioDuPdfRelieLesEtapesParCode() {
        WorkflowDefinitionResponseDto r = service.creer(requete(
                etape(9, "VAL_DFI", "ENGAGEMENT_VALIDER_DFI", "0.00", "APP_DG", null),
                etape(10, "APP_DG", "ENGAGEMENT_APPROUVER_DG", "50000.01", "CONTRAT", "EXCEPTION"),
                etape(11, "EXCEPTION", "ENGAGEMENT_ANNULER_CG", null, null, null),
                etape(12, "CONTRAT", "CONTRAT_ETABLIR", null, null, null)));

        assertTrue(r.getActif()); // actif par défaut
        assertEquals(4, r.getEtapes().size());
        WorkflowEtapeResponseDto dfi = r.getEtapes().get(0);
        WorkflowEtapeResponseDto dg = r.getEtapes().get(1);
        assertEquals(dg.getId(), dfi.getEtapeSuivanteSuccesId());
        assertEquals(r.getEtapes().get(3).getId(), dg.getEtapeSuivanteSuccesId());
        assertEquals(r.getEtapes().get(2).getId(), dg.getEtapeSuivanteRejetId());
        assertNull(dfi.getEtapeSuivanteRejetId());
        assertEquals(new BigDecimal("50000.01"), dg.getMontantMinimum());
    }

    @Test
    void codeDeWorkflowDejaUtiliseRefuse() {
        when(repository.existsByCode("CHAINE_ENGAGEMENT")).thenReturn(true);
        assertThrows(WorkflowException.class, () -> service.creer(requete(etape(1, "A", "P", null, null, null))));
    }

    @Test
    void codeDEtapeEnDoubleRefuse() {
        assertThrows(WorkflowException.class, () -> service.creer(requete(etape(1, "A", "P", null, null, null), etape(2, "A", "P", null, null, null))));
    }

    @Test
    void renvoiVersUneEtapeInexistanteRefuse() {
        assertThrows(WorkflowException.class, () -> service.creer(requete(etape(1, "A", "P", null, "FANTOME", null))));
        assertThrows(WorkflowException.class, () -> service.creer(requete(etape(1, "A", "P", null, null, "FANTOME"))));
    }

    @Test
    void plageDeMontantsIncoherenteRefusee() {
        WorkflowEtapeRequestDto e = etape(1, "A", "P", "500", null, null);
        e.setMontantMaximum(new BigDecimal("100"));
        assertThrows(WorkflowException.class, () -> service.creer(requete(e)));

        assertThrows(WorkflowException.class, () -> service.creer(requete(etape(1, "B", "P", "-1", null, null))));
    }

    @Test
    void renvoiParIdentifiantRefuseAvecMessageExplicite() {
        UUID existant = UUID.randomUUID();
        WorkflowEtapeRequestDto e = etape(1, "A", "P", null, null, null);
        e.setEtapeSuivanteSuccesId(existant);

        when(etapeRepository.existsById(existant)).thenReturn(true);
        assertTrue(assertThrows(WorkflowException.class, () -> service.creer(requete(e))).getMessage().contains("propre définition"));

        UUID inconnu = UUID.randomUUID();
        e.setEtapeSuivanteSuccesId(inconnu);
        assertTrue(assertThrows(WorkflowException.class, () -> service.creer(requete(e))).getMessage().contains("Code"));
    }
}
