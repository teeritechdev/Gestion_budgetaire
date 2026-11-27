package com.budget.backend.achat.service;

import com.budget.backend.achat.AchatException;
import com.budget.backend.achat.dto.BonCommandeResponseDto;
import com.budget.backend.achat.dto.CreerBcDepuisDaDto;
import com.budget.backend.achat.entity.BonCommande;
import com.budget.backend.achat.entity.DemandeAchat;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.entity.LigneBonCommande;
import com.budget.backend.achat.entity.LigneDemandeAchat;
import com.budget.backend.achat.repository.BonCommandeRepository;
import com.budget.backend.achat.repository.DemandeAchatRepository;
import com.budget.backend.achat.repository.FournisseurRepository;
import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import com.budget.backend.stock.service.NumeroDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
public class BonCommandeService {

    @Autowired
    private BonCommandeRepository repository;

    @Autowired
    private DemandeAchatRepository daRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private NumeroDocumentService numeroDocumentService;

    @Transactional(readOnly = true)
    public PageResponseDto<BonCommandeResponseDto> filtrer(
            String statut, UUID fournisseurId, Integer annee, String recherche, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (statut != null && !statut.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("statut")), statut.trim().toUpperCase()));
            }
            if (fournisseurId != null) {
                predicates.add(cb.equal(root.get("fournisseur").get("id"), fournisseurId));
            }
            if (annee != null) {
                LocalDate debut = LocalDate.of(annee, 1, 1);
                LocalDate fin = LocalDate.of(annee, 12, 31);
                predicates.add(cb.between(root.get("dateEmission"), debut, fin));
            }
            if (recherche != null && !recherche.isBlank()) {
                String term = "%" + recherche.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("codeBc")), term),
                        cb.like(cb.lower(root.get("fournisseur").get("raisonSociale")), term),
                        cb.like(cb.lower(root.get("demandeAchat").get("codeDossier")), term),
                        cb.like(cb.lower(root.get("demandeAchat").get("objet")), term)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), BonCommandeResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<BonCommandeResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(BonCommandeResponseDto::from);
    }

    @Transactional
    public BonCommandeResponseDto creerDepuisDa(UUID daId, CreerBcDepuisDaDto dto) {
        DemandeAchat da = daRepository.findById(daId)
                .orElseThrow(() -> new AchatException("Demande d'achat introuvable avec l'ID : " + daId));

        if (!DemandeAchat.APPROUVE.equals(da.getStatut())) {
            throw new AchatException("Seule une demande d'achat avec le statut APPROUVE peut faire l'objet d'un bon de commande.");
        }

        if (repository.existsByDemandeAchat_Id(daId)) {
            throw new AchatException("Un bon de commande a déjà été généré pour la demande d'achat " + da.getCodeDossier());
        }

        Fournisseur fournisseur = fournisseurRepository.findById(dto.getFournisseurId())
                .orElseThrow(() -> new AchatException("Fournisseur introuvable avec l'ID : " + dto.getFournisseurId()));

        if (fournisseur.getActif() != null && !fournisseur.getActif()) {
            throw new AchatException("Le fournisseur sélectionné est inactif.");
        }

        String codeBc = numeroDocumentService.prochainCodeBonCommande();
        String typeBc = dto.getTypeBc() != null && !dto.getTypeBc().isBlank() ? dto.getTypeBc().trim().toUpperCase() : "CLASSIQUE";

        BigDecimal montantTotalHt = BigDecimal.ZERO;
        var lignesBc = new ArrayList<LigneBonCommande>();

        BonCommande bc = BonCommande.builder()
                .codeBc(codeBc)
                .typeBc(typeBc)
                .demandeAchat(da)
                .fournisseur(fournisseur)
                .statut(BonCommande.EMIS)
                .dateEmission(LocalDate.now())
                .montantTotalHt(BigDecimal.ZERO)
                .montantTotalTtc(BigDecimal.ZERO)
                .montantFactureTtc(BigDecimal.ZERO)
                .lignes(lignesBc)
                .build();

        if (da.getLignes() != null) {
            for (LigneDemandeAchat lda : da.getLignes()) {
                BigDecimal ligneTotalHt = lda.getMontantTotalHt() != null ? lda.getMontantTotalHt() : BigDecimal.ZERO;
                montantTotalHt = montantTotalHt.add(ligneTotalHt);

                LigneBonCommande lbc = LigneBonCommande.builder()
                        .bonCommande(bc)
                        .ligneDa(lda)
                        .article(lda.getArticle())
                        .description(lda.getDescription())
                        .quantiteCommandee(lda.getQuantite())
                        .prixUnitaireHt(lda.getPrixUnitaireEstime())
                        .montantTotalHt(ligneTotalHt)
                        .build();
                lignesBc.add(lbc);
            }
        }

        BigDecimal tauxTva = dto.getTauxTva() != null ? dto.getTauxTva() : BigDecimal.valueOf(18);
        BigDecimal tvaMultiplier = BigDecimal.ONE.add(tauxTva.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal montantTotalTtc = montantTotalHt.multiply(tvaMultiplier).setScale(2, RoundingMode.HALF_UP);

        bc.setMontantTotalHt(montantTotalHt);
        bc.setMontantTotalTtc(montantTotalTtc);

        bc = repository.save(bc);

        // Mettre à jour le statut de la DA
        da.setStatut(DemandeAchat.COMMANDE);
        da.setEtapeCourante("COMMANDE");
        daRepository.save(da);

        return BonCommandeResponseDto.from(bc);
    }

    @Transactional(readOnly = true)
    public byte[] genererPdf(UUID id) {
        BonCommande bc = repository.findById(id)
                .orElseThrow(() -> new AchatException("Bon de commande introuvable avec l'ID : " + id));
        return BonCommandePdfGenerator.generer(bc);
    }
}

