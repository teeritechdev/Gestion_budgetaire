package com.budget.backend.achat.service;

import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import org.springframework.data.domain.Pageable;
import com.budget.backend.achat.AchatException;
import com.budget.backend.achat.api.BudgetReservationApi;
import com.budget.backend.achat.dto.BonCommandeCreateDto;
import com.budget.backend.achat.dto.BonCommandeResponseDto;
import com.budget.backend.achat.dto.LigneBCRequestDto;
import com.budget.backend.achat.entity.BonCommande;
import com.budget.backend.achat.entity.DemandeAchat;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.entity.LigneBonCommande;
import com.budget.backend.achat.entity.LigneDemandeAchat;
import com.budget.backend.achat.repository.BonCommandeRepository;
import com.budget.backend.achat.repository.DemandeAchatRepository;
import com.budget.backend.achat.repository.FournisseurRepository;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.stock.entity.Article;
import com.budget.backend.stock.repository.ArticleRepository;
import com.budget.backend.stock.service.AuditHelper;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.NumeroDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bon de commande : créé à partir d'une DA APPROUVE (la DA passe alors à COMMANDE).
 * Les crédits ont été engagés à la soumission de la DA ; si le BC est inférieur à l'estimation,
 * la différence est libérée (type ENGAGEMENT_BC) au prorata des lignes budgétaires engagées.
 */
@Service
public class BonCommandeService {

    @Autowired
    private BonCommandeRepository repository;
    @Autowired
    private DemandeAchatRepository demandeAchatRepository;
    @Autowired
    private FournisseurRepository fournisseurRepository;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private DemandeAchatService demandeAchatService;
    @Autowired
    private CurrentUserService currentUserService;
    @Autowired
    private NumeroDocumentService numeroDocumentService;
    @Autowired
    private BudgetReservationApi budgetApi;
    @Autowired
    private AuditHelper auditHelper;

    @Value("${achat.tva-taux:0.18}")
    private BigDecimal tvaTaux;

    // --- Consultation ---

    @Transactional(readOnly = true)
    public PageResponseDto<BonCommandeResponseDto> filtrer(String codeBc, UUID fournisseurId, String statut, UUID daId, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (codeBc != null && !codeBc.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("codeBc")), "%" + codeBc.toLowerCase() + "%"));
            }
            if (fournisseurId != null) {
                predicates.add(cb.equal(root.get("fournisseur").get("id"), fournisseurId));
            }
            if (statut != null && !statut.isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("statut")), statut.toUpperCase()));
            }
            if (daId != null) {
                predicates.add(cb.equal(root.get("demandeAchat").get("id"), daId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), BonCommandeResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<BonCommandeResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(BonCommandeResponseDto::from);
    }

    // --- Création ---

    @Transactional
    public BonCommandeResponseDto creer(BonCommandeCreateDto dto) {
        DemandeAchat da = demandeAchatRepository.findByIdForUpdate(dto.getDaId())
                .orElseThrow(() -> new AchatException("Demande d'achat introuvable avec l'ID : " + dto.getDaId()));
        if (!DemandeAchat.APPROUVE.equals(da.getStatut())) {
            throw new AchatException("Un bon de commande ne peut être créé que pour une demande APPROUVE (statut actuel : "
                    + da.getStatut() + ").");
        }
        if (repository.existsByDemandeAchatId(da.getId())) {
            throw new AchatException("Un bon de commande existe déjà pour la demande " + da.getCodeDossier() + ".");
        }
        Fournisseur fournisseur = fournisseurRepository.findById(dto.getFournisseurId())
                .orElseThrow(() -> new AchatException("Fournisseur introuvable avec l'ID : " + dto.getFournisseurId()));
        if (!Boolean.TRUE.equals(fournisseur.getActif())) {
            throw new AchatException("Le fournisseur " + fournisseur.getCodeFournisseur() + " est suspendu.");
        }
        String type = dto.getTypeBc() == null || dto.getTypeBc().isBlank() ? BonCommande.CLASSIQUE : dto.getTypeBc().trim().toUpperCase();
        if (!BonCommande.CLASSIQUE.equals(type) && !BonCommande.CADRE_ANNUEL.equals(type)) {
            throw new AchatException("Type de bon de commande invalide : '" + dto.getTypeBc() + "' (attendu : CLASSIQUE ou CADRE_ANNUEL).");
        }
        Utilisateur auteur = currentUserService.utilisateurConnecte();

        BonCommande bc = BonCommande.builder()
                .codeBc(numeroDocumentService.prochainCodeBonCommande())
                .typeBc(type)
                .demandeAchat(da)
                .fournisseur(fournisseur)
                .statut(BonCommande.EMIS)
                .dateEmission(LocalDate.now())
                .montantFactureTtc(BigDecimal.ZERO)
                .build();

        BigDecimal totalHt = BigDecimal.ZERO;
        for (LigneBCRequestDto ligneDto : dto.getLignes()) {
            LigneDemandeAchat ligneDa = da.getLignes().stream()
                    .filter(l -> l.getId().equals(ligneDto.getLigneDaId()))
                    .findFirst()
                    .orElseThrow(() -> new AchatException("La ligne de DA " + ligneDto.getLigneDaId()
                            + " n'appartient pas à la demande " + da.getCodeDossier() + "."));
            BigDecimal montantLigne = montantLigne(ligneDto);
            Article article = resoudreArticle(ligneDto, ligneDa);
            bc.getLignes().add(LigneBonCommande.builder()
                    .bonCommande(bc)
                    .ligneDa(ligneDa)
                    .article(article)
                    .description(ligneDto.getDescription())
                    .quantiteCommandee(ligneDto.getQuantiteCommandee())
                    .prixUnitaireHt(ligneDto.getPrixUnitaireHt())
                    .montantTotalHt(montantLigne)
                    .build());
            totalHt = totalHt.add(montantLigne);
        }
        if (totalHt.compareTo(da.getMontantEstimeHt()) > 0) {
            throw new AchatException("Le montant du bon de commande (" + totalHt + ") dépasse l'estimation de la demande ("
                    + da.getMontantEstimeHt() + ") : les crédits engagés ne le couvrent pas.");
        }
        bc.setMontantTotalHt(totalHt);
        bc.setMontantTotalTtc(totalHt.multiply(BigDecimal.ONE.add(tvaTaux)).setScale(2, RoundingMode.HALF_UP));

        // Prix négocié inférieur à l'estimation : on rend le surplus de crédits engagés
        libererEcart(da, da.getMontantEstimeHt().subtract(totalHt), bc.getCodeBc(), auteur);

        da.setStatut(DemandeAchat.COMMANDE);
        da.setEtapeCourante(null);
        demandeAchatRepository.save(da);
        bc = repository.saveAndFlush(bc);
        auditHelper.tracer(auteur, AuditHelper.INSERTION, "bons_commande", bc.getId(), null,
                AuditHelper.valeurs("codeBc", bc.getCodeBc(), "codeDossier", da.getCodeDossier(),
                        "fournisseur", fournisseur.getCodeFournisseur(), "montantTotalHt", bc.getMontantTotalHt(),
                        "montantTotalTtc", bc.getMontantTotalTtc()));
        return BonCommandeResponseDto.from(bc);
    }

    /** EMIS -> ENVOYE_FOURNISSEUR (ou CADRE_EN_COURS pour un contrat cadre). */
    @Transactional
    public BonCommandeResponseDto emettre(UUID id) {
        BonCommande bc = repository.findById(id)
                .orElseThrow(() -> new AchatException("Bon de commande introuvable avec l'ID : " + id));
        if (!BonCommande.EMIS.equals(bc.getStatut())) {
            throw new AchatException("Seul un bon de commande au statut EMIS peut être envoyé (statut actuel : " + bc.getStatut() + ").");
        }
        String statutAvant = bc.getStatut();
        bc.setStatut(BonCommande.CADRE_ANNUEL.equals(bc.getTypeBc()) ? BonCommande.CADRE_EN_COURS : BonCommande.ENVOYE_FOURNISSEUR);
        bc = repository.saveAndFlush(bc);
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.MODIFICATION, "bons_commande", bc.getId(),
                AuditHelper.valeurs("statut", statutAvant), AuditHelper.valeurs("statut", bc.getStatut()));
        return BonCommandeResponseDto.from(bc);
    }

    // --- Facturation (appelée par le Module 7) ---

    /**
     * Impute une facture (montant TTC) sur le bon de commande : cumule montant_facture_ttc et fait évoluer le statut
     * (PARTIELLEMENT_FACTURE, puis SOLDE quand tout est facturé ; un contrat cadre reste CADRE_EN_COURS tant qu'il
     * n'est pas soldé). Refuse un BC non envoyé, annulé ou soldé, et un cumul supérieur au montant TTC du BC.
     */
    @Transactional
    public BonCommandeResponseDto imputerFacture(UUID bcId, BigDecimal montantTtc) {
        if (montantTtc == null || montantTtc.signum() <= 0) {
            throw new AchatException("Le montant facturé doit être strictement positif.");
        }
        BonCommande bc = repository.findByIdForUpdate(bcId)
                .orElseThrow(() -> new AchatException("Bon de commande introuvable avec l'ID : " + bcId));
        if (BonCommande.EMIS.equals(bc.getStatut()) || BonCommande.ANNULE.equals(bc.getStatut())
                || BonCommande.SOLDE.equals(bc.getStatut())) {
            throw new AchatException("Le bon de commande " + bc.getCodeBc() + " (statut " + bc.getStatut()
                    + ") ne peut pas recevoir de facture.");
        }
        BigDecimal dejaFacture = bc.getMontantFactureTtc() == null ? BigDecimal.ZERO : bc.getMontantFactureTtc();
        BigDecimal nouveauCumul = dejaFacture.add(montantTtc);
        if (nouveauCumul.compareTo(bc.getMontantTotalTtc()) > 0) {
            throw new AchatException("La facture dépasse le solde du bon de commande " + bc.getCodeBc() + " : solde "
                    + bc.calculerSolde() + ", facture " + montantTtc + ".");
        }
        String statutAvant = bc.getStatut();
        bc.setMontantFactureTtc(nouveauCumul);
        if (nouveauCumul.compareTo(bc.getMontantTotalTtc()) == 0) {
            bc.setStatut(BonCommande.SOLDE);
        } else if (!BonCommande.CADRE_ANNUEL.equals(bc.getTypeBc())) {
            bc.setStatut(BonCommande.PARTIELLEMENT_FACTURE);
        }
        bc = repository.saveAndFlush(bc);
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.MODIFICATION, "bons_commande", bc.getId(),
                AuditHelper.valeurs("statut", statutAvant, "montantFactureTtc", dejaFacture),
                AuditHelper.valeurs("statut", bc.getStatut(), "montantFactureTtc", nouveauCumul));
        return BonCommandeResponseDto.from(bc);
    }

    // --- PDF ---

    @Transactional(readOnly = true)
    public byte[] genererPdf(UUID id) {
        BonCommande bc = repository.findById(id)
                .orElseThrow(() -> new AchatException("Bon de commande introuvable avec l'ID : " + id));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titre = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18);
            document.add(new com.lowagie.text.Paragraph("BON DE COMMANDE " + bc.getCodeBc(), titre));
            document.add(new com.lowagie.text.Paragraph(" "));
            document.add(new com.lowagie.text.Paragraph("Date d'émission : " + bc.getDateEmission()));
            document.add(new com.lowagie.text.Paragraph("Dossier d'achat : " + bc.getDemandeAchat().getCodeDossier()
                    + " - " + bc.getDemandeAchat().getObjet()));
            Fournisseur f = bc.getFournisseur();
            document.add(new com.lowagie.text.Paragraph("Fournisseur : " + f.getRaisonSociale() + " (" + f.getCodeFournisseur()
                    + ", IFU " + f.getIfu() + ")"));
            document.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{5f, 1.5f, 2.5f, 2.5f});
            table.addCell("Désignation");
            table.addCell("Quantité");
            table.addCell("Prix unitaire HT");
            table.addCell("Montant HT");
            for (LigneBonCommande l : bc.getLignes()) {
                table.addCell(l.getDescription());
                table.addCell(l.getQuantiteCommandee() != null ? l.getQuantiteCommandee().stripTrailingZeros().toPlainString() : "Forfait");
                table.addCell(l.getPrixUnitaireHt() != null ? l.getPrixUnitaireHt().toPlainString() : "-");
                table.addCell(l.getMontantTotalHt().toPlainString());
            }
            document.add(table);
            document.add(new com.lowagie.text.Paragraph(" "));
            document.add(new com.lowagie.text.Paragraph("Total HT : " + bc.getMontantTotalHt().toPlainString()));
            document.add(new com.lowagie.text.Paragraph("Total TTC : " + bc.getMontantTotalTtc().toPlainString()));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new AchatException("Erreur lors de la génération du PDF du bon de commande : " + e.getMessage());
        }
    }

    // --- Règles de calcul ---

    /** quantité x prix si fournis, sinon montant forfaitaire obligatoire. */
    private BigDecimal montantLigne(LigneBCRequestDto l) {
        if (l.getQuantiteCommandee() != null && l.getPrixUnitaireHt() != null) {
            BigDecimal montant = l.getPrixUnitaireHt().multiply(l.getQuantiteCommandee());
            return montant.scale() > 2 ? montant.setScale(2, RoundingMode.HALF_UP) : montant;
        }
        if (l.getQuantiteCommandee() != null || l.getPrixUnitaireHt() != null) {
            throw new AchatException("Renseignez à la fois la quantité et le prix unitaire, ou seulement le montant total forfaitaire.");
        }
        if (l.getMontantTotalHt() == null) {
            throw new AchatException("Une ligne sans quantité ni prix unitaire doit indiquer son montant total HT.");
        }
        return l.getMontantTotalHt();
    }

    private Article resoudreArticle(LigneBCRequestDto l, LigneDemandeAchat ligneDa) {
        if (l.getArticleId() == null) {
            return ligneDa.getArticle();
        }
        return articleRepository.findById(l.getArticleId())
                .orElseThrow(() -> new AchatException("Article introuvable : " + l.getArticleId()));
    }

    /** Libère l'écart au prorata de l'engagement de chaque ligne ; la dernière absorbe l'arrondi. */
    private void libererEcart(DemandeAchat da, BigDecimal ecart, String referenceBc, Utilisateur auteur) {
        if (ecart.signum() <= 0) {
            return;
        }
        Map<UUID, BigDecimal> repartition = demandeAchatService.repartitionEngagement(da);
        BigDecimal base = da.getMontantEstimeHt();
        BigDecimal restant = ecart;
        int i = 0;
        for (Map.Entry<UUID, BigDecimal> e : repartition.entrySet()) {
            i++;
            BigDecimal part = (i == repartition.size())
                    ? restant
                    : e.getValue().multiply(ecart).divide(base, 2, RoundingMode.HALF_UP);
            restant = restant.subtract(part);
            if (part.signum() > 0) {
                budgetApi.liberer(e.getKey(), part, BudgetReservationApi.ENGAGEMENT_BC, referenceBc,
                        da.getExerciceAnnee(), auteur.getId());
            }
        }
    }
}
