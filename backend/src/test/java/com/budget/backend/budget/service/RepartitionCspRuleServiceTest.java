package com.budget.backend.budget.service;

import com.budget.backend.budget.dto.RepartitionCspRuleRequestDto;
import com.budget.backend.budget.entity.RepartitionCspCleType;
import com.budget.backend.budget.entity.RepartitionCspRule;
import com.budget.backend.budget.entity.BudgetAllocationActivite;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.entity.ActiviteCatalogue;
import com.budget.backend.budget.repository.RepartitionCspRuleRepository;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.UniteOrganisationnelle;
import com.budget.backend.security.repository.StructureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepartitionCspRuleServiceTest {
    private final UUID allocationId = UUID.randomUUID();
    private UUID allocationActivityId;

    @Mock
    private RepartitionCspRuleRepository repository;

    @Mock
    private StructureRepository entiteRepository;

    @Mock
    private BudgetAllocationStructureRepository allocationStructureRepository;

    @InjectMocks
    private RepartitionCspRuleService service;

    @Test
    void enregistreLePourcentageTransmisPourUneEntite() {
        UUID catalogueActivityId = UUID.randomUUID();
        stubCspAllocation(catalogueActivityId);
        when(repository.save(any(RepartitionCspRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        RepartitionCspRuleRequestDto dto =
                request(allocationActivityId, allocationActivityId, "ENT-1", "0.255");

        RepartitionCspRule rule = service.creer(dto);

        assertEquals(new BigDecimal("0.255"), rule.getValeurCle());
        assertEquals("EFFECTIF", rule.getCleType());
    }

    @Test
    void enregistreUneRepartitionCompleteEtMetAJourLesReglesExistantes() {
        UUID catalogueActivityId = UUID.randomUUID();
        stubCspAllocation(catalogueActivityId);
        UUID beneficiaryActivityId = allocationActivityId;
        RepartitionCspRule existing = RepartitionCspRule.builder()
                .activiteCspId(allocationActivityId)
                .entiteBeneficiaire("ENT-1")
                .activiteBeneficiaireId(beneficiaryActivityId)
                .build();
        when(repository.findAllByAllocationStructureIdAndActiviteBeneficiaireId(allocationId, beneficiaryActivityId))
                .thenReturn(List.of(existing));
        when(repository.save(any(RepartitionCspRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(entiteRepository.findAllById(any()))
                .thenReturn(List.of(beneficiary("ENT-1"), beneficiary("ENT-2")));

        List<RepartitionCspRule> result = service.enregistrerRepartition(List.of(
                request(allocationActivityId, beneficiaryActivityId, "ENT-1", "0.25"),
                request(allocationActivityId, beneficiaryActivityId, "ENT-2", "0.75")));

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("0.25"), result.get(0).getValeurCle());
        assertEquals(new BigDecimal("0.75"), result.get(1).getValeurCle());
        verify(repository).save(existing);
    }

    @Test
    void refuseQueLaStructureSourceSoitBeneficiaireDeSaPropreRepartition() {
        UUID catalogueActivityId = UUID.randomUUID();
        stubCspAllocation(catalogueActivityId);
        UUID beneficiaryActivityId = allocationActivityId;
        when(entiteRepository.findAllById(any()))
                .thenReturn(List.of(Structure.builder().code("CSP").build()));

        assertThrows(
                ResponseStatusException.class,
                () -> service.enregistrerRepartition(List.of(
                        request(allocationActivityId, beneficiaryActivityId, "CSP", "1"))));
    }

    @Test
    void refuseUnTotalDeRatiosDifferentDeUn() {
        UUID catalogueActivityId = UUID.randomUUID();
        stubCspAllocation(catalogueActivityId);
        UUID beneficiaryActivityId = allocationActivityId;
        when(entiteRepository.findAllById(any()))
                .thenReturn(List.of(beneficiary("ENT-1"), beneficiary("ENT-2")));
        assertThrows(
                ResponseStatusException.class,
                () -> service.enregistrerRepartition(List.of(
                        request(allocationActivityId, beneficiaryActivityId, "ENT-1", "0.25"),
                        request(allocationActivityId, beneficiaryActivityId, "ENT-2", "0.70"))));
    }

    @Test
    void refuseUnRatioSuperieurAUn() {
        UUID catalogueActivityId = UUID.randomUUID();
        stubCspAllocation(catalogueActivityId);
        RepartitionCspRuleRequestDto dto =
                request(allocationActivityId, allocationActivityId, "ENT-1", "1.001");
        assertThrows(ResponseStatusException.class, () -> service.creer(dto));
    }

    @Test
    void refuseLIdentifiantDuCatalogueAuLieuDeLaCleEtrangereAllocation() {
        UUID catalogueActivityId = UUID.randomUUID();
        stubCspAllocation(catalogueActivityId);
        RepartitionCspRuleRequestDto dto =
                request(catalogueActivityId, allocationActivityId, "ENT-1", "1");

        assertThrows(ResponseStatusException.class, () -> service.creer(dto));
    }

    @Test
    void modifieUneRegleExistante() {
        UUID catalogueActivityId = UUID.randomUUID();
        stubCspAllocation(catalogueActivityId);
        UUID ruleId = UUID.randomUUID();
        RepartitionCspRule existing = RepartitionCspRule.builder()
                .id(ruleId)
                .build();
        when(repository.findById(ruleId)).thenReturn(Optional.of(existing));
        when(repository.save(any(RepartitionCspRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        RepartitionCspRuleRequestDto dto =
                request(allocationActivityId, allocationActivityId, "ENT-1", "1");

        RepartitionCspRule updated = service.modifier(ruleId, dto);

        assertEquals(ruleId, updated.getId());
        assertEquals("ENT-1", updated.getEntiteBeneficiaire());
        assertEquals(new BigDecimal("1"), updated.getValeurCle());
    }

    @Test
    void renvoieNotFoundQuandLaRegleAModifierNExistePas() {
        UUID ruleId = UUID.randomUUID();
        UUID catalogueActivityId = UUID.randomUUID();
        stubCspAllocation(catalogueActivityId);
        when(repository.findById(ruleId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.modifier(
                        ruleId,
                        request(allocationActivityId, allocationActivityId, "ENT-1", "1")));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void supprimeUneRegleExistante() {
        UUID ruleId = UUID.randomUUID();
        when(repository.existsById(ruleId)).thenReturn(true);

        service.supprimer(ruleId);

        verify(repository).deleteById(ruleId);
    }

    @Test
    void renvoieNotFoundQuandLaRegleASupprimerNExistePas() {
        UUID ruleId = UUID.randomUUID();

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.supprimer(ruleId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private RepartitionCspRuleRequestDto request(
            UUID activityId, UUID beneficiaryActivityId, String entite, String percentage) {
        RepartitionCspRuleRequestDto dto = new RepartitionCspRuleRequestDto();
        dto.setActiviteCspId(activityId);
        dto.setAllocationStructureId(allocationId);
        dto.setEntiteBeneficiaire(entite);
        dto.setActiviteBeneficiaireId(beneficiaryActivityId);
        dto.setCleType(RepartitionCspCleType.EFFECTIF);
        dto.setValeurCle(new BigDecimal(percentage));
        return dto;
    }

    private void stubCspAllocation(UUID activityId) {
        ActiviteCatalogue activity = ActiviteCatalogue.builder()
                .id(activityId)
                .codeActivite("CSP-1")
                .libelle("Activité CSP")
                .build();
        allocationActivityId = UUID.randomUUID();
        BudgetAllocationActivite allocationActivite = BudgetAllocationActivite.builder()
                .id(allocationActivityId)
                .activiteCatalogue(activity)
                .build();
        Structure structure = Structure.builder()
                .code("CSP")
                .nom("Structure CSP")
                
                .build();
        when(allocationStructureRepository.findById(allocationId))
                .thenReturn(Optional.of(BudgetAllocationStructure.builder()
                        .id(allocationId)
                        .structure(structure)
                        .budgetAllocationActivite(allocationActivite)
                        .build()));
    }

    private Structure beneficiary(String code) {
        return Structure.builder().code(code).build();
    }
}
