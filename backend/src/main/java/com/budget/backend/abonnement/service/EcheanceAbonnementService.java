package com.budget.backend.abonnement.service;

import com.budget.backend.abonnement.AbonnementException;
import com.budget.backend.abonnement.dto.EcheanceResponseDto;
import com.budget.backend.abonnement.dto.EcheanceUpdateDto;
import com.budget.backend.abonnement.entity.EcheanceAbonnement;
import com.budget.backend.abonnement.repository.EcheanceAbonnementRepository;
import com.budget.backend.achat.entity.BonCommande;
import com.budget.backend.achat.repository.BonCommandeRepository;
import com.budget.backend.stock.service.AuditHelper;
import com.budget.backend.stock.service.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Cycle d'une échéance : A_VENIR -> BC_GENERE -> FACTURE_RECUE -> PAYE ; annulable tant qu'aucune facture n'est reçue. */
@Service
public class EcheanceAbonnementService {

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            EcheanceAbonnement.A_VENIR, Set.of(EcheanceAbonnement.BC_GENERE, EcheanceAbonnement.ANNULE),
            EcheanceAbonnement.BC_GENERE, Set.of(EcheanceAbonnement.FACTURE_RECUE, EcheanceAbonnement.ANNULE),
            EcheanceAbonnement.FACTURE_RECUE, Set.of(EcheanceAbonnement.PAYE));

    @Autowired
    private EcheanceAbonnementRepository repository;
    @Autowired
    private BonCommandeRepository bonCommandeRepository;
    @Autowired
    private CurrentUserService currentUserService;
    @Autowired
    private AuditHelper auditHelper;

    @Transactional(readOnly = true)
    public Optional<EcheanceResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(EcheanceResponseDto::from);
    }

    @Transactional
    public EcheanceResponseDto modifier(UUID id, EcheanceUpdateDto dto) {
        EcheanceAbonnement e = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new AbonnementException("Echéance introuvable avec l'ID : " + id));
        String cible = AbonnementService.normaliser(dto.getStatut());
        String actuel = e.getStatut();
        if (!TRANSITIONS.getOrDefault(actuel, Set.of()).contains(cible)) {
            throw new AbonnementException("Changement de statut interdit : " + actuel + " -> " + cible + ".");
        }

        Map<String, Object> avant = AuditHelper.valeurs("statut", actuel, "montantFactureTtc", e.getMontantFactureTtc(),
                "referenceFacture", e.getReferenceFacture());

        switch (cible) {
            case EcheanceAbonnement.BC_GENERE -> {
                if (dto.getBcId() == null) {
                    throw new AbonnementException("Le bon de commande (bcId) est obligatoire pour passer à BC_GENERE.");
                }
                BonCommande bc = bonCommandeRepository.findById(dto.getBcId())
                        .orElseThrow(() -> new AbonnementException("Bon de commande introuvable : " + dto.getBcId()));
                if (!bc.getFournisseur().getId().equals(e.getAbonnement().getFournisseur().getId())) {
                    throw new AbonnementException("Le bon de commande " + bc.getCodeBc()
                            + " n'est pas émis au fournisseur de cet abonnement.");
                }
                e.setBonCommande(bc);
            }
            case EcheanceAbonnement.FACTURE_RECUE -> {
                if (dto.getReferenceFacture() == null || dto.getReferenceFacture().isBlank()) {
                    throw new AbonnementException("La référence de la facture est obligatoire pour passer à FACTURE_RECUE.");
                }
                if (dto.getMontantFactureTtc() == null) {
                    throw new AbonnementException("Le montant TTC facturé est obligatoire pour passer à FACTURE_RECUE.");
                }
                e.setReferenceFacture(dto.getReferenceFacture());
                e.setMontantFactureTtc(dto.getMontantFactureTtc());
            }
            default -> { /* PAYE et ANNULE ne portent pas de données supplémentaires */ }
        }
        e.setStatut(cible);
        e = repository.saveAndFlush(e);

        BigDecimal ecart = e.ecartTtc();
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.MODIFICATION, "echeances_abonnement", e.getId(), avant,
                AuditHelper.valeurs("statut", e.getStatut(), "montantFactureTtc", e.getMontantFactureTtc(),
                        "referenceFacture", e.getReferenceFacture(), "ecartTtc", ecart));
        return EcheanceResponseDto.from(e);
    }
}
