package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.BudgetAllocationActivite;
import com.budget.backend.budget.entity.BudgetAllocationOp;
import com.budget.backend.budget.entity.BudgetAllocationOs;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.entity.DemandeRevisionBudgetaire;
import com.budget.backend.budget.entity.ExerciceBudgetaire;
import com.budget.backend.budget.entity.JournalMouvementsBudgetaire;
import com.budget.backend.budget.entity.LigneRevisionBudgetaire;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.budget.repository.DemandeRevisionBudgetaireRepository;
import com.budget.backend.budget.repository.JournalMouvementsBudgetaireRepository;
import com.budget.backend.budget.repository.LigneRevisionBudgetaireRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevisionBudgetaireServiceTest {
    private final UUID requestId = UUID.randomUUID();
    private final UUID validatorId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();

    @Mock
    private DemandeRevisionBudgetaireRepository demandeRepository;
    @Mock
    private LigneRevisionBudgetaireRepository ligneRepository;
    @Mock
    private JournalMouvementsBudgetaireRepository journalRepository;
    @Mock
    private BudgetAllocationStructureRepository allocationRepository;

    @InjectMocks
    private RevisionBudgetaireService service;

    @Test
    void listeUniquementLesDemandesDuDemandeurAvecSesFiltres() {
        UUID demandeurId = UUID.randomUUID();
        List<DemandeRevisionBudgetaire> demandes = List.of(demande("BROUILLON"));
        when(demandeRepository.findByDemandeurAndFilters(demandeurId, 2026, "BROUILLON"))
                .thenReturn(demandes);

        assertEquals(demandes, service.listerParDemandeur(demandeurId, 2026, "BROUILLON"));

        verify(demandeRepository).findByDemandeurAndFilters(demandeurId, 2026, "BROUILLON");
    }

    @Test
    void rejetteSansModifierLesAllocationsNiCreerDeMouvements() {
        DemandeRevisionBudgetaire demande = demande("EN_ATTENTE");
        when(demandeRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(demande));
        when(demandeRepository.save(any(DemandeRevisionBudgetaire.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DemandeRevisionBudgetaire result =
                service.validerDemande(requestId, validatorId, "REJETE", "Demande non justifiée");

        assertEquals("REJETE", result.getStatut());
        assertEquals(validatorId, result.getValideurId());
        assertEquals("Demande non justifiée", result.getCommentaireValidation());
        verify(ligneRepository, never()).findByDemandeRevisionBudgetaireId(requestId);
        verify(allocationRepository, never()).save(any(BudgetAllocationStructure.class));
        verify(journalRepository, never()).save(any(JournalMouvementsBudgetaire.class));
    }

    @Test
    void valideEtAppliqueDebitCreditEtJournalisation() {
        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        BudgetAllocationStructure source = allocation(sourceId, "100", "0", "10", "0", 2026);
        BudgetAllocationStructure destination = allocation(destinationId, "50", "5", "0", "0", 2026);
        DemandeRevisionBudgetaire demande = demande("EN_ATTENTE");
        LigneRevisionBudgetaire ligne = LigneRevisionBudgetaire.builder()
                .typeMouvement("VIREMENT_INTERNE")
                .montant(new BigDecimal("40"))
                .allocationSource(source)
                .allocationDestination(destination)
                .build();

        when(demandeRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(demande));
        when(ligneRepository.findByDemandeRevisionBudgetaireId(requestId)).thenReturn(List.of(ligne));
        when(allocationRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
        when(allocationRepository.findByIdForUpdate(destinationId)).thenReturn(Optional.of(destination));
        when(demandeRepository.save(any(DemandeRevisionBudgetaire.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationRepository.save(any(BudgetAllocationStructure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(journalRepository.save(any(JournalMouvementsBudgetaire.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DemandeRevisionBudgetaire result =
                service.validerDemande(requestId, validatorId, "VALIDE", "Accordée");

        assertEquals("VALIDE", result.getStatut());
        assertEquals(new BigDecimal("-40"), source.getMontantModifie());
        assertEquals(new BigDecimal("45"), destination.getMontantModifie());
        verify(journalRepository, org.mockito.Mockito.times(2)).save(any(JournalMouvementsBudgetaire.class));

        ArgumentCaptor<JournalMouvementsBudgetaire> mouvements =
                ArgumentCaptor.forClass(JournalMouvementsBudgetaire.class);
        verify(journalRepository, org.mockito.Mockito.times(2)).save(mouvements.capture());
        JournalMouvementsBudgetaire mouvementSource = mouvements.getAllValues().stream()
                .filter(mouvement -> mouvement.getAllocationStructure().getId().equals(sourceId))
                .findFirst()
                .orElseThrow();
        JournalMouvementsBudgetaire mouvementDestination = mouvements.getAllValues().stream()
                .filter(mouvement -> mouvement.getAllocationStructure().getId().equals(destinationId))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("100"), mouvementSource.getMontantAnterieur());
        assertEquals(new BigDecimal("60"), mouvementSource.getNouveauMontant());
        assertEquals(new BigDecimal("55"), mouvementDestination.getMontantAnterieur());
        assertEquals(new BigDecimal("95"), mouvementDestination.getNouveauMontant());
    }

    @Test
    void refuseUnVirementQuiDepasseLeDisponible() {
        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        BudgetAllocationStructure source = allocation(sourceId, "100", "0", "70", "0", 2026);
        BudgetAllocationStructure destination = allocation(destinationId, "50", "0", "0", "0", 2026);
        LigneRevisionBudgetaire ligne = LigneRevisionBudgetaire.builder()
                .typeMouvement("VIREMENT_INTERNE")
                .montant(new BigDecimal("40"))
                .allocationSource(source)
                .allocationDestination(destination)
                .build();
        when(demandeRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(demande("EN_ATTENTE")));
        when(ligneRepository.findByDemandeRevisionBudgetaireId(requestId)).thenReturn(List.of(ligne));
        when(allocationRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
        when(allocationRepository.findByIdForUpdate(destinationId)).thenReturn(Optional.of(destination));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validerDemande(requestId, validatorId, "VALIDE", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(allocationRepository, never()).save(any(BudgetAllocationStructure.class));
        verify(journalRepository, never()).save(any(JournalMouvementsBudgetaire.class));
    }

    @Test
    void empecheDeTraiterUneDemandeDejaDecidee() {
        when(demandeRepository.findByIdForUpdate(requestId))
                .thenReturn(Optional.of(demande("REJETE")));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validerDemande(requestId, validatorId, "VALIDE", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void soumetUnBrouillonAvecAuMoinsUneLigne() {
        DemandeRevisionBudgetaire demande = demande("BROUILLON");
        when(demandeRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(demande));
        when(ligneRepository.findByDemandeRevisionBudgetaireId(requestId))
                .thenReturn(List.of(LigneRevisionBudgetaire.builder().build()));
        when(demandeRepository.save(any(DemandeRevisionBudgetaire.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DemandeRevisionBudgetaire result = service.soumettreDemande(requestId, requesterId);

        assertEquals("EN_ATTENTE", result.getStatut());
    }

    @Test
    void refuseDeDeciderUnBrouillonNonSoumis() {
        when(demandeRepository.findByIdForUpdate(requestId))
                .thenReturn(Optional.of(demande("BROUILLON")));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validerDemande(requestId, validatorId, "VALIDE", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(ligneRepository, never()).findByDemandeRevisionBudgetaireId(requestId);
        verify(allocationRepository, never()).save(any(BudgetAllocationStructure.class));
        verify(journalRepository, never()).save(any(JournalMouvementsBudgetaire.class));
    }

    @Test
    void supprimeUniquementLesBrouillonsEtLeursLignes() {
        DemandeRevisionBudgetaire demande = demande("BROUILLON");
        LigneRevisionBudgetaire ligne = LigneRevisionBudgetaire.builder().build();
        when(demandeRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(demande));
        when(ligneRepository.findByDemandeRevisionBudgetaireId(requestId)).thenReturn(List.of(ligne));

        service.supprimerDemandeBrouillon(requestId, requesterId);

        verify(ligneRepository).deleteAll(List.of(ligne));
        verify(demandeRepository).delete(demande);
    }

    @Test
    void interditAuDemandeurDeDeciderDeSaPropreDemande() {
        when(demandeRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(demande("EN_ATTENTE")));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validerDemande(requestId, requesterId, "VALIDE", null));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(demandeRepository, never()).save(any(DemandeRevisionBudgetaire.class));
    }

    @Test
    void refuseDeSoumettreLaDemandeDUnAutreUtilisateur() {
        when(demandeRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(demande("BROUILLON")));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.soumettreDemande(requestId, UUID.randomUUID()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(demandeRepository, never()).save(any(DemandeRevisionBudgetaire.class));
    }

    @Test
    void refuseDeSupprimerLeBrouillonDUnAutreUtilisateur() {
        when(demandeRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(demande("BROUILLON")));

        assertThrows(ResponseStatusException.class,
                () -> service.supprimerDemandeBrouillon(requestId, UUID.randomUUID()));

        verify(demandeRepository, never()).delete(any(DemandeRevisionBudgetaire.class));
    }

    @Test
    void refuseDAppliquerUneRevisionSurUnExerciceCloture() {
        UUID destinationId = UUID.randomUUID();
        BudgetAllocationStructure destination = allocation(destinationId, "50", "0", "0", "0", 2026);
        destination.getBudgetAllocationActivite().getBudgetAllocationOp().getBudgetAllocationOs()
                .getExerciceBudgetaire().setStatut("CLOTURE");
        LigneRevisionBudgetaire ligne = LigneRevisionBudgetaire.builder()
                .typeMouvement("EXTENSION")
                .montant(new BigDecimal("10"))
                .allocationDestination(destination)
                .build();
        when(demandeRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(demande("EN_ATTENTE")));
        when(ligneRepository.findByDemandeRevisionBudgetaireId(requestId)).thenReturn(List.of(ligne));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validerDemande(requestId, validatorId, "VALIDE", null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(allocationRepository, never()).save(any(BudgetAllocationStructure.class));
    }

    @Test
    void unValideurVoitLesDemandesSoumisesMaisPasLesBrouillonsDesAutres() {
        DemandeRevisionBudgetaire brouillonAutre = demande("BROUILLON");
        DemandeRevisionBudgetaire soumiseAutre = demande("EN_ATTENTE");

        assertEquals(false, RevisionBudgetaireService.estVisible(brouillonAutre, validatorId, true));
        assertEquals(true, RevisionBudgetaireService.estVisible(soumiseAutre, validatorId, true));
        assertEquals(false, RevisionBudgetaireService.estVisible(soumiseAutre, validatorId, false));
        assertEquals(true, RevisionBudgetaireService.estVisible(brouillonAutre, requesterId, false));
    }

    private DemandeRevisionBudgetaire demande(String statut) {
        return DemandeRevisionBudgetaire.builder()
                .id(requestId)
                .numeroDemande("REV-TEST")
                .anneeExercice(2026)
                .demandeurId(requesterId)
                .statut(statut)
                .build();
    }

    private BudgetAllocationStructure allocation(
            UUID id, String vote, String modified, String engaged, String liquidated, int year) {
        ExerciceBudgetaire exercise = ExerciceBudgetaire.builder().annee(year).build();
        BudgetAllocationOs os = BudgetAllocationOs.builder().exerciceBudgetaire(exercise).build();
        BudgetAllocationOp op = BudgetAllocationOp.builder().budgetAllocationOs(os).build();
        BudgetAllocationActivite activity = BudgetAllocationActivite.builder().budgetAllocationOp(op).build();
        return BudgetAllocationStructure.builder()
                .id(id)
                .budgetAllocationActivite(activity)
                .montantVote(new BigDecimal(vote))
                .montantModifie(new BigDecimal(modified))
                .montantEngage(new BigDecimal(engaged))
                .montantLiquide(new BigDecimal(liquidated))
                .build();
    }
}
