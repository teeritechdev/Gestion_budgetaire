package com.budget.backend.achat.api;

import com.budget.backend.achat.AchatException;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.entity.JournalMouvementsBudgetaire;
import com.budget.backend.budget.repository.JournalMouvementsBudgetaireRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * IMPLÉMENTATION PROVISOIRE de BudgetReservationApi, en attendant celle du Module 2.
 * N'utilise que les entités et repositories existants ; calcule le disponible en Java (sans dépendre
 * de la colonne générée montant_disponible).
 */
@Service
public class BudgetReservationProvisoire implements BudgetReservationApi {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private JournalMouvementsBudgetaireRepository journalRepository;

    @Override
    @Transactional
    public void engager(UUID allocationId, BigDecimal montant, String typeMouvement,
                        String referenceDocument, Integer anneeExercice, UUID auteurId) {
        verifierMontant(montant);
        BudgetAllocationStructure ligne = verrouiller(allocationId);
        BigDecimal disponible = calculerDisponible(ligne);
        if (montant.compareTo(disponible) > 0) {
            throw new AchatException("Budget insuffisant sur la ligne " + allocationId + " : disponible "
                    + disponible + ", demandé " + montant);
        }
        BigDecimal ancien = valeur(ligne.getMontantEngage());
        ligne.setMontantEngage(ancien.add(montant));
        tracer(ligne, typeMouvement, ancien, montant, ligne.getMontantEngage(), referenceDocument, anneeExercice, auteurId);
    }

    @Override
    @Transactional
    public void liberer(UUID allocationId, BigDecimal montant, String typeMouvement,
                        String referenceDocument, Integer anneeExercice, UUID auteurId) {
        verifierMontant(montant);
        BudgetAllocationStructure ligne = verrouiller(allocationId);
        BigDecimal ancien = valeur(ligne.getMontantEngage());
        if (montant.compareTo(ancien) > 0) {
            throw new AchatException("Impossible de libérer " + montant + " : seulement " + ancien
                    + " engagés sur la ligne " + allocationId);
        }
        ligne.setMontantEngage(ancien.subtract(montant));
        tracer(ligne, typeMouvement, ancien, montant.negate(), ligne.getMontantEngage(), referenceDocument, anneeExercice, auteurId);
    }

    @Override
    @Transactional
    public void liquider(UUID allocationId, BigDecimal montant, String referenceDocument,
                         Integer anneeExercice, UUID auteurId) {
        verifierMontant(montant);
        BudgetAllocationStructure ligne = verrouiller(allocationId);
        BigDecimal engage = valeur(ligne.getMontantEngage());
        if (montant.compareTo(engage) > 0) {
            throw new AchatException("Impossible de liquider " + montant + " : seulement " + engage
                    + " engagés sur la ligne " + allocationId + ".");
        }
        BigDecimal liquideAvant = valeur(ligne.getMontantLiquide());
        ligne.setMontantEngage(engage.subtract(montant));
        ligne.setMontantLiquide(liquideAvant.add(montant));
        // le journal suit la colonne qui augmente : montant_liquide
        tracer(ligne, LIQUIDATION_FACTURE, liquideAvant, montant, ligne.getMontantLiquide(), referenceDocument, anneeExercice, auteurId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal disponible(UUID allocationId) {
        BudgetAllocationStructure ligne = em.find(BudgetAllocationStructure.class, allocationId);
        if (ligne == null) {
            throw new AchatException("Ligne budgétaire introuvable avec l'ID : " + allocationId);
        }
        return calculerDisponible(ligne);
    }

    /**
     * Verrouille la ligne et en relit l'état à jour. Un simple find avec verrou renverrait l'instance déjà chargée
     * dans cette transaction (donc potentiellement périmée si une autre transaction vient de l'engager) :
     * il faut flusher nos propres modifications puis rafraîchir sous verrou.
     */
    private BudgetAllocationStructure verrouiller(UUID allocationId) {
        BudgetAllocationStructure ligne = em.find(BudgetAllocationStructure.class, allocationId);
        if (ligne == null) {
            throw new AchatException("Ligne budgétaire introuvable avec l'ID : " + allocationId);
        }
        em.flush();
        em.refresh(ligne, LockModeType.PESSIMISTIC_WRITE);
        return ligne;
    }

    private BigDecimal calculerDisponible(BudgetAllocationStructure l) {
        return valeur(l.getMontantVote()).add(valeur(l.getMontantModifie()))
                .subtract(valeur(l.getMontantEngage()).add(valeur(l.getMontantLiquide())));
    }

    private BigDecimal valeur(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void verifierMontant(BigDecimal montant) {
        if (montant == null || montant.signum() <= 0) {
            throw new AchatException("Le montant doit être strictement positif.");
        }
    }

    private void tracer(BudgetAllocationStructure ligne, String type, BigDecimal ancien, BigDecimal variation,
                        BigDecimal nouveau, String reference, Integer annee, UUID auteurId) {
        journalRepository.save(JournalMouvementsBudgetaire.builder()
                .allocationStructure(ligne)
                .anneeExercice(annee)
                .typeMouvement(type)
                .montantAnterieur(ancien)
                .variation(variation)
                .nouveauMontant(nouveau)
                .referenceDocument(reference)
                .auteurId(auteurId)
                .build());
    }
}
