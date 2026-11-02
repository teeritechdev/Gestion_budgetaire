package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.BudgetAllocationActivite;
import com.budget.backend.budget.entity.BudgetAllocationOp;
import com.budget.backend.budget.entity.BudgetAllocationOs;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.entity.ExerciceBudgetaire;
import com.budget.backend.budget.entity.ObjectifStrategique;
import com.budget.backend.budget.repository.ActiviteCatalogueRepository;
import com.budget.backend.budget.repository.BudgetAllocationActiviteRepository;
import com.budget.backend.budget.repository.BudgetAllocationOpRepository;
import com.budget.backend.budget.repository.BudgetAllocationOsRepository;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.budget.repository.ExerciceBudgetaireRepository;
import com.budget.backend.budget.repository.ObjectifOperationnelRepository;
import com.budget.backend.budget.repository.ObjectifStrategiqueRepository;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.StructureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetAllocationStructureServiceTest {
    private final UUID activiteId = UUID.randomUUID();
    private final UUID allocationId = UUID.randomUUID();

    @Mock
    private BudgetAllocationStructureRepository repository;
    @Mock
    private BudgetAllocationActiviteRepository activiteAllocationRepository;
    @Mock
    private StructureRepository structureRepo;

    @InjectMocks
    private BudgetAllocationStructureService service;

    @Test
    void refuseDeDepasserLEnveloppeDeLActivite() {
        when(activiteAllocationRepository.findByIdForUpdate(activiteId)).thenReturn(Optional.of(activite("100")));
        when(structureRepo.findById("DIR-IT")).thenReturn(Optional.of(Structure.builder().code("DIR-IT").build()));
        when(repository.findByBudgetAllocationActiviteId(activiteId)).thenReturn(List.of(
                BudgetAllocationStructure.builder().montantVote(new BigDecimal("90")).build()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.attribuerBudgetStructure(activiteId, "DIR-IT", new BigDecimal("20")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(any(BudgetAllocationStructure.class));
    }

    @Test
    void refuseUneExecutionQuiDepasseLeMontantAlloue() {
        BudgetAllocationStructure allocation = BudgetAllocationStructure.builder()
                .id(allocationId)
                .budgetAllocationActivite(activite("100"))
                .montantVote(new BigDecimal("100"))
                .montantModifie(new BigDecimal("-20"))
                .build();
        when(repository.findByIdForUpdate(allocationId)).thenReturn(Optional.of(allocation));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.mettreAJourExecution(allocationId, new BigDecimal("60"), new BigDecimal("30")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(any(BudgetAllocationStructure.class));
    }

    @Test
    void refuseUnMontantDExecutionNegatif() {
        assertThrows(ResponseStatusException.class,
                () -> service.mettreAJourExecution(allocationId, new BigDecimal("-1"), BigDecimal.ZERO));

        verifyNoInteractions(repository);
    }

    @Test
    void enregistreUneExecutionCompatibleAvecLeDisponible() {
        BudgetAllocationStructure allocation = BudgetAllocationStructure.builder()
                .id(allocationId)
                .budgetAllocationActivite(activite("100"))
                .montantVote(new BigDecimal("100"))
                .montantModifie(BigDecimal.ZERO)
                .build();
        when(repository.findByIdForUpdate(allocationId)).thenReturn(Optional.of(allocation));
        when(repository.save(any(BudgetAllocationStructure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetAllocationStructure result =
                service.mettreAJourExecution(allocationId, new BigDecimal("60"), new BigDecimal("40"));

        assertEquals(new BigDecimal("60"), result.getMontantEngage());
        assertEquals(new BigDecimal("40"), result.getMontantLiquide());
    }

    private BudgetAllocationActivite activite(String montant) {
        ExerciceBudgetaire exercice = ExerciceBudgetaire.builder().annee(2026).statut("OUVERT").build();
        BudgetAllocationOs os = BudgetAllocationOs.builder().exerciceBudgetaire(exercice).build();
        BudgetAllocationOp op = BudgetAllocationOp.builder().budgetAllocationOs(os).build();
        return BudgetAllocationActivite.builder()
                .id(activiteId)
                .budgetAllocationOp(op)
                .montantAlloue(new BigDecimal(montant))
                .build();
    }
}
