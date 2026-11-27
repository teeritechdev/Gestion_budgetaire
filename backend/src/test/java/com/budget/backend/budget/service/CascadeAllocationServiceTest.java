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
class CascadeAllocationServiceTest {
    private final UUID exerciceId = UUID.randomUUID();
    private final UUID osId = UUID.randomUUID();

    @Mock
    private ExerciceBudgetaireRepository exerciceRepository;
    @Mock
    private ObjectifStrategiqueRepository osRepository;
    @Mock
    private ObjectifOperationnelRepository opRepository;
    @Mock
    private ActiviteCatalogueRepository activiteCatalogueRepository;
    @Mock
    private BudgetAllocationOsRepository allocationOsRepository;
    @Mock
    private BudgetAllocationOpRepository allocationOpRepository;
    @Mock
    private BudgetAllocationActiviteRepository allocationActiviteRepository;

    @InjectMocks
    private CascadeAllocationService service;

    @Test
    void refuseUnMontantNegatifOuNulSansToucherALaBase() {
        assertThrows(ResponseStatusException.class,
                () -> service.allouerExerciceVersOs(exerciceId, osId, new BigDecimal("-10")));
        assertThrows(ResponseStatusException.class,
                () -> service.allouerExerciceVersOs(exerciceId, osId, null));

        verifyNoInteractions(exerciceRepository, allocationOsRepository);
    }

    @Test
    void refuseDeDepasserLEnveloppeVoteeDeLExercice() {
        when(exerciceRepository.findByIdForUpdate(exerciceId)).thenReturn(Optional.of(exercice("OUVERT", "100")));
        when(osRepository.findById(osId)).thenReturn(Optional.of(ObjectifStrategique.builder().id(osId).build()));
        when(allocationOsRepository.findByExerciceBudgetaireId(exerciceId)).thenReturn(List.of(
                BudgetAllocationOs.builder().montantAlloue(new BigDecimal("80")).build()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.allouerExerciceVersOs(exerciceId, osId, new BigDecimal("30")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(allocationOsRepository, never()).save(any(BudgetAllocationOs.class));
    }

    @Test
    void refuseDAllouerSurUnExerciceCloture() {
        when(exerciceRepository.findByIdForUpdate(exerciceId)).thenReturn(Optional.of(exercice("CLOTURE", "100")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.allouerExerciceVersOs(exerciceId, osId, new BigDecimal("10")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(allocationOsRepository, never()).save(any(BudgetAllocationOs.class));
    }

    @Test
    void alloueDansLEnveloppeDisponible() {
        ExerciceBudgetaire exercice = exercice("OUVERT", "100");
        when(exerciceRepository.findByIdForUpdate(exerciceId)).thenReturn(Optional.of(exercice));
        when(osRepository.findById(osId)).thenReturn(Optional.of(ObjectifStrategique.builder().id(osId).build()));
        when(allocationOsRepository.findByExerciceBudgetaireId(exerciceId)).thenReturn(List.of(
                BudgetAllocationOs.builder().montantAlloue(new BigDecimal("70")).build()));
        when(allocationOsRepository.save(any(BudgetAllocationOs.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BudgetAllocationOs result = service.allouerExerciceVersOs(exerciceId, osId, new BigDecimal("30"));

        assertEquals(new BigDecimal("30"), result.getMontantAlloue());
        assertEquals(exercice, result.getExerciceBudgetaire());
    }

    private ExerciceBudgetaire exercice(String statut, String montantVote) {
        return ExerciceBudgetaire.builder()
                .id(exerciceId)
                .annee(2026)
                .statut(statut)
                .montantTotalVote(new BigDecimal(montantVote))
                .build();
    }
}
