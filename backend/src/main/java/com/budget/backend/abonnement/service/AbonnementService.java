package com.budget.backend.abonnement.service;

import com.budget.backend.abonnement.AbonnementException;
import com.budget.backend.abonnement.dto.AbonnementRequestDto;
import com.budget.backend.abonnement.dto.AbonnementResponseDto;
import com.budget.backend.abonnement.dto.EcheanceAbonnementResponseDto;
import com.budget.backend.abonnement.dto.RapprochementFactureDto;
import com.budget.backend.abonnement.entity.Abonnement;
import com.budget.backend.abonnement.entity.EcheanceAbonnement;
import com.budget.backend.abonnement.repository.AbonnementRepository;
import com.budget.backend.abonnement.repository.EcheanceAbonnementRepository;
import com.budget.backend.achat.entity.BonCommande;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.entity.LigneBonCommande;
import com.budget.backend.achat.repository.BonCommandeRepository;
import com.budget.backend.achat.repository.FournisseurRepository;
import com.budget.backend.budget.entity.ActiviteCatalogue;
import com.budget.backend.budget.repository.ActiviteCatalogueRepository;
import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.NumeroDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AbonnementService {

    @Autowired
    private AbonnementRepository repository;

    @Autowired
    private EcheanceAbonnementRepository echeanceRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private ActiviteCatalogueRepository activiteRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired(required = false)
    private com.budget.backend.workflow.service.WorkflowService workflowService;

    @Autowired
    private NumeroDocumentService numeroDocumentService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private BonCommandeRepository bonCommandeRepository;

    @Transactional(readOnly = true)
    public PageResponseDto<AbonnementResponseDto> filtrer(
            String statut, UUID fournisseurId, UUID responsableId, String recherche, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (statut != null && !statut.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("statut")), statut.trim().toUpperCase()));
            }
            if (fournisseurId != null) {
                predicates.add(cb.equal(root.get("fournisseur").get("id"), fournisseurId));
            }
            if (responsableId != null) {
                predicates.add(cb.equal(root.get("responsable").get("id"), responsableId));
            }
            if (recherche != null && !recherche.isBlank()) {
                String term = "%" + recherche.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("codeAbonnement")), term),
                        cb.like(cb.lower(root.get("libelle")), term),
                        cb.like(cb.lower(root.get("fournisseur").get("raisonSociale")), term)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), AbonnementResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<AbonnementResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(AbonnementResponseDto::from);
    }

    @Transactional
    public AbonnementResponseDto creer(AbonnementRequestDto dto) {
        Fournisseur fournisseur = fournisseurRepository.findById(dto.getFournisseurId())
                .orElseThrow(() -> new AbonnementException("Fournisseur introuvable avec l'ID : " + dto.getFournisseurId()));

        ActiviteCatalogue activite = null;
        if (dto.getActiviteId() != null) {
            activite = activiteRepository.findById(dto.getActiviteId())
                    .orElseThrow(() -> new AbonnementException("Activité budgétaire introuvable avec l'ID : " + dto.getActiviteId()));
        }

        if (dto.getDateFin() != null && dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new AbonnementException("La date de fin ne peut pas être antérieure à la date de début.");
        }

        String code = dto.getCodeAbonnement();
        if (code == null || code.isBlank()) {
            code = numeroDocumentService.prochainCodeAbonnement();
        } else if (repository.existsByCodeAbonnement(code.trim())) {
            throw new AbonnementException("Un abonnement avec le code " + code + " existe déjà.");
        }

        BigDecimal montantTtc = dto.getMontantEcheanceTtc();
        if (montantTtc == null || montantTtc.compareTo(BigDecimal.ZERO) <= 0) {
            montantTtc = dto.getMontantEcheanceHt().multiply(new BigDecimal("1.18")).setScale(2, RoundingMode.HALF_UP);
        }

        Utilisateur responsable = null;
        if (dto.getResponsableId() != null) {
            responsable = utilisateurRepository.findById(dto.getResponsableId()).orElse(null);
        }
        if (responsable == null) {
            responsable = currentUserService.utilisateurConnecte();
        }

        Abonnement abn = Abonnement.builder()
                .codeAbonnement(code)
                .libelle(dto.getLibelle().trim())
                .fournisseur(fournisseur)
                .activite(activite)
                .periodicite(dto.getPeriodicite() != null ? dto.getPeriodicite().trim().toUpperCase() : "MENSUEL")
                .jourFacturation(dto.getJourFacturation() != null ? dto.getJourFacturation() : 31)
                .montantEcheanceHt(dto.getMontantEcheanceHt())
                .montantEcheanceTtc(montantTtc)
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .taciteReconduction(Boolean.TRUE.equals(dto.getTaciteReconduction()))
                .preavisJours(dto.getPreavisJours() != null ? dto.getPreavisJours() : 30)
                .statut(dto.getStatut() != null && !dto.getStatut().isBlank() ? dto.getStatut().trim().toUpperCase() : Abonnement.ACTIF)
                .responsable(responsable)
                .echeances(new ArrayList<>())
                .build();

        abn = repository.save(abn);

        // Déclenchement automatique du circuit de workflow transversal s'il existe
        if (workflowService != null) {
            try {
                workflowService.demarrerWorkflowSiActif(
                        "CONTRAT_ABONNEMENT",
                        abn.getId(),
                        abn.getCodeAbonnement(),
                        abn.getLibelle(),
                        abn.getMontantEcheanceTtc(),
                        abn.getResponsable()
                );
            } catch (Exception e) {
                System.err.println("Workflow non déclenché pour Abonnement " + abn.getCodeAbonnement() + ": " + e.getMessage());
            }
        }

        // Génération automatique des échéances prévisionnelles
        genererEcheancesInternes(abn);

        return AbonnementResponseDto.from(abn);
    }

    @Transactional
    public AbonnementResponseDto modifier(UUID id, AbonnementRequestDto dto) {
        Abonnement abn = repository.findById(id)
                .orElseThrow(() -> new AbonnementException("Abonnement introuvable avec l'ID : " + id));

        Fournisseur fournisseur = fournisseurRepository.findById(dto.getFournisseurId())
                .orElseThrow(() -> new AbonnementException("Fournisseur introuvable : " + dto.getFournisseurId()));

        ActiviteCatalogue activite = null;
        if (dto.getActiviteId() != null) {
            activite = activiteRepository.findById(dto.getActiviteId())
                    .orElseThrow(() -> new AbonnementException("Activité budgétaire introuvable : " + dto.getActiviteId()));
        }

        if (dto.getDateFin() != null && dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new AbonnementException("La date de fin ne peut pas être antérieure à la date de début.");
        }

        BigDecimal montantTtc = dto.getMontantEcheanceTtc();
        if (montantTtc == null || montantTtc.compareTo(BigDecimal.ZERO) <= 0) {
            montantTtc = dto.getMontantEcheanceHt().multiply(new BigDecimal("1.18")).setScale(2, RoundingMode.HALF_UP);
        }

        abn.setLibelle(dto.getLibelle().trim());
        abn.setFournisseur(fournisseur);
        abn.setActivite(activite);
        abn.setPeriodicite(dto.getPeriodicite().trim().toUpperCase());
        abn.setJourFacturation(dto.getJourFacturation() != null ? dto.getJourFacturation() : 31);
        abn.setMontantEcheanceHt(dto.getMontantEcheanceHt());
        abn.setMontantEcheanceTtc(montantTtc);
        abn.setDateDebut(dto.getDateDebut());
        abn.setDateFin(dto.getDateFin());
        abn.setTaciteReconduction(Boolean.TRUE.equals(dto.getTaciteReconduction()));
        abn.setPreavisJours(dto.getPreavisJours() != null ? dto.getPreavisJours() : 30);
        if (dto.getStatut() != null && !dto.getStatut().isBlank()) {
            abn.setStatut(dto.getStatut().trim().toUpperCase());
        }

        abn = repository.save(abn);
        return AbonnementResponseDto.from(abn);
    }

    @Transactional
    public void supprimer(UUID id) {
        Abonnement abn = repository.findById(id)
                .orElseThrow(() -> new AbonnementException("Abonnement introuvable avec l'ID : " + id));

        boolean aDesBcs = abn.getEcheances().stream().anyMatch(e -> e.getBonCommande() != null);
        if (aDesBcs) {
            throw new AbonnementException("Impossible de supprimer un abonnement pour lequel des bons de commande ont déjà été émis.");
        }

        repository.delete(abn);
    }

    @Transactional
    public List<EcheanceAbonnementResponseDto> genererEcheancier(UUID abonnementId) {
        Abonnement abn = repository.findById(abonnementId)
                .orElseThrow(() -> new AbonnementException("Abonnement introuvable avec l'ID : " + abonnementId));

        boolean aDesBcs = abn.getEcheances().stream().anyMatch(e -> e.getBonCommande() != null);
        if (aDesBcs) {
            throw new AbonnementException("Impossible de réinitialiser l'échéancier car des bons de commande y sont déjà adossés.");
        }

        abn.getEcheances().clear();
        genererEcheancesInternes(abn);
        repository.save(abn);

        return echeanceRepository.findByAbonnement_IdOrderByNumEcheanceAsc(abonnementId).stream()
                .map(EcheanceAbonnementResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EcheanceAbonnementResponseDto> obtenirEcheances(UUID abonnementId) {
        return echeanceRepository.findByAbonnement_IdOrderByNumEcheanceAsc(abonnementId).stream()
                .map(EcheanceAbonnementResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<EcheanceAbonnementResponseDto> filtrerToutesEcheances(
            String statut, UUID abonnementId, Pageable pageable) {
        return PageResponseDto.from(echeanceRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (statut != null && !statut.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("statut")), statut.trim().toUpperCase()));
            }
            if (abonnementId != null) {
                predicates.add(cb.equal(root.get("abonnement").get("id"), abonnementId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), EcheanceAbonnementResponseDto::from);
    }

    private void genererEcheancesInternes(Abonnement abn) {
        LocalDate cur = abn.getDateDebut();
        LocalDate fin = abn.getDateFin() != null ? abn.getDateFin() : abn.getDateDebut().plusYears(1);
        int jour = abn.getJourFacturation() != null ? abn.getJourFacturation() : 31;
        int num = 1;

        while (!cur.isAfter(fin)) {
            int maxDaysInMonth = cur.lengthOfMonth();
            int actualDay = Math.min(jour, maxDaysInMonth);
            LocalDate echeanceDate = cur.withDayOfMonth(actualDay);

            if (echeanceDate.isBefore(abn.getDateDebut())) {
                echeanceDate = abn.getDateDebut();
            }
            if (echeanceDate.isAfter(fin)) {
                break;
            }

            EcheanceAbonnement ech = EcheanceAbonnement.builder()
                    .abonnement(abn)
                    .numEcheance(num++)
                    .dateEcheance(echeanceDate)
                    .montantAttenduTtc(abn.getMontantEcheanceTtc())
                    .montantFactureTtc(BigDecimal.ZERO)
                    .statut(EcheanceAbonnement.A_VENIR)
                    .build();

            abn.getEcheances().add(ech);

            cur = switch (abn.getPeriodicite().toUpperCase()) {
                case "TRIMESTRIEL" -> cur.plusMonths(3);
                case "SEMESTRIEL" -> cur.plusMonths(6);
                case "ANNUEL" -> cur.plusYears(1);
                default -> cur.plusMonths(1);
            };
        }
    }

    @Transactional
    public EcheanceAbonnementResponseDto genererBcPourEcheance(UUID abonnementId, UUID echeanceId) {
        Abonnement abn = repository.findById(abonnementId)
                .orElseThrow(() -> new AbonnementException("Abonnement introuvable avec l'ID : " + abonnementId));

        EcheanceAbonnement ech = echeanceRepository.findById(echeanceId)
                .orElseThrow(() -> new AbonnementException("Échéance introuvable avec l'ID : " + echeanceId));

        if (!ech.getAbonnement().getId().equals(abonnementId)) {
            throw new AbonnementException("L'échéance sélectionnée n'appartient pas à cet abonnement.");
        }

        if (ech.getBonCommande() != null) {
            throw new AbonnementException("Un bon de commande (" + ech.getBonCommande().getCodeBc() + ") a déjà été émis pour cette échéance.");
        }

        String codeBc = numeroDocumentService.prochainCodeBonCommande();
        var lignes = new ArrayList<LigneBonCommande>();

        BigDecimal ht = abn.getMontantEcheanceHt() != null ? abn.getMontantEcheanceHt() : BigDecimal.ZERO;
        BigDecimal ttc = ech.getMontantAttenduTtc() != null ? ech.getMontantAttenduTtc() : ht.multiply(new BigDecimal("1.18")).setScale(2, RoundingMode.HALF_UP);

        BonCommande bc = BonCommande.builder()
                .codeBc(codeBc)
                .typeBc("ABONNEMENT")
                .fournisseur(abn.getFournisseur())
                .statut(BonCommande.EMIS)
                .dateEmission(LocalDate.now())
                .montantTotalHt(ht)
                .montantTotalTtc(ttc)
                .montantFactureTtc(BigDecimal.ZERO)
                .lignes(lignes)
                .build();

        LigneBonCommande lbc = LigneBonCommande.builder()
                .bonCommande(bc)
                .description("Échéance n°" + ech.getNumEcheance() + " - " + abn.getLibelle() + " (Date d'exigibilité : " + ech.getDateEcheance() + ")")
                .quantiteCommandee(1)
                .prixUnitaireHt(ht)
                .montantTotalHt(ht)
                .build();
        lignes.add(lbc);

        bc = bonCommandeRepository.save(bc);

        ech.setBonCommande(bc);
        ech.setStatut(EcheanceAbonnement.BON_COMMANDE_GENERE);
        ech = echeanceRepository.save(ech);

        return EcheanceAbonnementResponseDto.from(ech);
    }

    @Transactional
    public EcheanceAbonnementResponseDto rapprocherFacture(UUID abonnementId, UUID echeanceId, RapprochementFactureDto dto) {
        Abonnement abn = repository.findById(abonnementId)
                .orElseThrow(() -> new AbonnementException("Abonnement introuvable avec l'ID : " + abonnementId));

        EcheanceAbonnement ech = echeanceRepository.findById(echeanceId)
                .orElseThrow(() -> new AbonnementException("Échéance introuvable avec l'ID : " + echeanceId));

        if (!ech.getAbonnement().getId().equals(abonnementId)) {
            throw new AbonnementException("L'échéance sélectionnée n'appartient pas à cet abonnement.");
        }

        ech.setReferenceFacture(dto.getReferenceFacture().trim());
        ech.setMontantFactureTtc(dto.getMontantFactureTtc());
        ech.setStatut(EcheanceAbonnement.FACTURE);

        // Si un BC est rattaché à l'échéance, synchroniser le montant facturé et solder le BC si le montant est couvert
        if (ech.getBonCommande() != null) {
            BonCommande bc = ech.getBonCommande();
            bc.setMontantFactureTtc(dto.getMontantFactureTtc());
            if (dto.getMontantFactureTtc().compareTo(bc.getMontantTotalTtc()) >= 0) {
                bc.setStatut(BonCommande.SOLDE);
            }
            bonCommandeRepository.save(bc);
        }

        ech = echeanceRepository.save(ech);
        return EcheanceAbonnementResponseDto.from(ech);
    }
}
