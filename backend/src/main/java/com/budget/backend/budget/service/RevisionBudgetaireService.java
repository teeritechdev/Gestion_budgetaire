package com.budget.backend.budget.service;

import com.budget.backend.common.CsvUtils;
import com.budget.backend.budget.dto.LigneRevisionRequestDto;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.entity.DemandeRevisionBudgetaire;
import com.budget.backend.budget.entity.ExerciceBudgetaire;
import com.budget.backend.budget.entity.JournalMouvementsBudgetaire;
import com.budget.backend.budget.entity.LigneRevisionBudgetaire;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.budget.repository.DemandeRevisionBudgetaireRepository;
import com.budget.backend.budget.repository.JournalMouvementsBudgetaireRepository;
import com.budget.backend.budget.repository.LigneRevisionBudgetaireRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class RevisionBudgetaireService {

    static final String STATUT_BROUILLON = "BROUILLON";
    static final String STATUT_EXERCICE_CLOTURE = "CLOTURE";

    @Autowired
    private DemandeRevisionBudgetaireRepository demandeRepository;

    @Autowired
    private LigneRevisionBudgetaireRepository ligneRepository;

    @Autowired
    private JournalMouvementsBudgetaireRepository journalRepository;

    @Autowired
    private BudgetAllocationStructureRepository allocationStructureRepository;

    public List<DemandeRevisionBudgetaire> listerToutes() {
        return demandeRepository.findAll();
    }

    public List<DemandeRevisionBudgetaire> listerParDemandeur(
            UUID demandeurId, Integer anneeExercice, String statut) {
        return demandeRepository.findByDemandeurAndFilters(demandeurId, anneeExercice, statut);
    }

    public Optional<DemandeRevisionBudgetaire> trouverParId(UUID id) {
        return demandeRepository.findById(id);
    }

    public Optional<DemandeRevisionBudgetaire> trouverParIdEtDemandeur(UUID id, UUID demandeurId) {
        return demandeRepository.findByIdAndDemandeurId(id, demandeurId);
    }

    /**
     * Demandes visibles par un utilisateur : les siennes, et, s'il a le droit de valider,
     * toutes les demandes soumises ou décidées des autres (jamais leurs brouillons).
     */
    public List<DemandeRevisionBudgetaire> listerVisibles(
            UUID utilisateurId, boolean peutValider, Integer anneeExercice, String statut) {
        if (!peutValider) {
            return listerParDemandeur(utilisateurId, anneeExercice, statut);
        }
        return demandeRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(visibilite(root, cb, utilisateurId, true));
            if (anneeExercice != null) {
                predicates.add(cb.equal(root.get("anneeExercice"), anneeExercice));
            }
            if (statut != null && !statut.isBlank()) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    public Optional<DemandeRevisionBudgetaire> trouverVisible(UUID id, UUID utilisateurId, boolean peutValider) {
        return demandeRepository.findById(id)
                .filter(demande -> estVisible(demande, utilisateurId, peutValider));
    }

    public static boolean estVisible(DemandeRevisionBudgetaire demande, UUID utilisateurId, boolean peutValider) {
        if (demande == null) {
            return false;
        }
        if (Objects.equals(demande.getDemandeurId(), utilisateurId)) {
            return true;
        }
        return peutValider && !STATUT_BROUILLON.equals(demande.getStatut());
    }

    private jakarta.persistence.criteria.Predicate visibilite(
            jakarta.persistence.criteria.Path<?> demande,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            UUID utilisateurId,
            boolean peutValider) {
        jakarta.persistence.criteria.Predicate proprietaire = cb.equal(demande.get("demandeurId"), utilisateurId);
        if (!peutValider) {
            return proprietaire;
        }
        return cb.or(proprietaire, cb.notEqual(demande.get("statut"), STATUT_BROUILLON));
    }

    private DemandeRevisionBudgetaire verrouillerDemandeDuDemandeur(UUID demandeId, UUID demandeurId) {
        DemandeRevisionBudgetaire demande = demandeRepository.findByIdForUpdate(demandeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Demande de révision introuvable avec l'ID : " + demandeId));
        if (!Objects.equals(demande.getDemandeurId(), demandeurId)) {
            // Même réponse qu'une demande inexistante : on ne révèle pas les demandes des autres.
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Demande de révision introuvable avec l'ID : " + demandeId);
        }
        return demande;
    }

    @Transactional
    public DemandeRevisionBudgetaire creerDemandeAvecLignes(DemandeRevisionBudgetaire demande, List<LigneRevisionRequestDto> lignesDto) {
        validerInformationsDemande(demande);
        List<LigneRevisionBudgetaire> lignes = preparerLignes(demande.getAnneeExercice(), lignesDto);

        demande.setStatut("BROUILLON");
        DemandeRevisionBudgetaire demandeSauvegardee = demandeRepository.save(demande);
        enregistrerLignes(demandeSauvegardee, lignes);
        return demandeSauvegardee;
    }

    @Transactional
    public DemandeRevisionBudgetaire modifierDemandeAvecLignes(
            UUID demandeId, DemandeRevisionBudgetaire modifications, List<LigneRevisionRequestDto> lignesDto) {
        DemandeRevisionBudgetaire demande = demandeRepository.findByIdForUpdate(demandeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Demande de révision introuvable avec l'ID : " + demandeId));
        if (!"BROUILLON".equals(demande.getStatut())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Seule une demande au statut brouillon peut être modifiée.");
        }

        validerInformationsDemande(modifications);
        List<LigneRevisionBudgetaire> nouvellesLignes =
                preparerLignes(modifications.getAnneeExercice(), lignesDto);

        demande.setNumeroDemande(modifications.getNumeroDemande());
        demande.setAnneeExercice(modifications.getAnneeExercice());
        demande.setDemandeurId(modifications.getDemandeurId());
        demande.setMotif(modifications.getMotif());
        ligneRepository.deleteAll(ligneRepository.findByDemandeRevisionBudgetaireId(demandeId));
        DemandeRevisionBudgetaire demandeSauvegardee = demandeRepository.save(demande);
        enregistrerLignes(demandeSauvegardee, nouvellesLignes);
        return demandeSauvegardee;
    }

    @Transactional
    public DemandeRevisionBudgetaire soumettreDemande(UUID demandeId, UUID demandeurId) {
        DemandeRevisionBudgetaire demande = verrouillerDemandeDuDemandeur(demandeId, demandeurId);
        if (!"BROUILLON".equals(demande.getStatut())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Seule une demande au statut brouillon peut être soumise.");
        }
        if (ligneRepository.findByDemandeRevisionBudgetaireId(demandeId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Une demande doit contenir au moins un mouvement avant sa soumission.");
        }
        demande.setStatut("EN_ATTENTE");
        return demandeRepository.save(demande);
    }

    @Transactional
    public void supprimerDemandeBrouillon(UUID demandeId, UUID demandeurId) {
        DemandeRevisionBudgetaire demande = verrouillerDemandeDuDemandeur(demandeId, demandeurId);
        if (!"BROUILLON".equals(demande.getStatut())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Seule une demande au statut brouillon peut être supprimée.");
        }
        ligneRepository.deleteAll(ligneRepository.findByDemandeRevisionBudgetaireId(demandeId));
        demandeRepository.delete(demande);
    }

    private void validerInformationsDemande(DemandeRevisionBudgetaire demande) {
        if (demande == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations de la demande sont obligatoires.");
        }
        if (demande.getNumeroDemande() == null || demande.getNumeroDemande().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le numéro de demande est obligatoire.");
        }
        demande.setNumeroDemande(demande.getNumeroDemande().trim());
        if (demande.getNumeroDemande().length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le numéro de demande ne peut pas dépasser 50 caractères.");
        }
        if (demande.getAnneeExercice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'exercice budgétaire est obligatoire.");
        }
        if (demande.getDemandeurId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le demandeur est obligatoire.");
        }
        if (demande.getMotif() == null || demande.getMotif().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif de la demande est obligatoire.");
        }
    }

    private List<LigneRevisionBudgetaire> preparerLignes(Integer anneeExercice, List<LigneRevisionRequestDto> lignesDto) {
        if (lignesDto == null || lignesDto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ajoutez au moins un mouvement à la demande.");
        }

        List<LigneRevisionBudgetaire> lignes = new ArrayList<>();
        for (LigneRevisionRequestDto ligneDto : lignesDto) {
            if (ligneDto == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un mouvement de la demande est invalide.");
            }
            BudgetAllocationStructure source = null;
            if (ligneDto.getAllocationSourceId() != null) {
                source = allocationStructureRepository.findById(ligneDto.getAllocationSourceId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "L'allocation source est introuvable."));
            }

            BudgetAllocationStructure destination = null;
            if (ligneDto.getAllocationDestinationId() != null) {
                destination = allocationStructureRepository.findById(ligneDto.getAllocationDestinationId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "L'allocation destination est introuvable."));
            }

            LigneRevisionBudgetaire ligne = LigneRevisionBudgetaire.builder()
                    .typeMouvement(ligneDto.getTypeMouvement())
                    .montant(ligneDto.getMontant())
                    .allocationSource(source)
                    .allocationDestination(destination)
                    .build();
            validerLigne(ligne, anneeExercice);
            lignes.add(ligne);
        }
        return lignes;
    }

    private void enregistrerLignes(DemandeRevisionBudgetaire demande, List<LigneRevisionBudgetaire> lignes) {
        for (LigneRevisionBudgetaire ligne : lignes) {
            ligne.setDemandeRevisionBudgetaire(demande);
            ligneRepository.save(ligne);
        }
    }

    @Transactional
    public DemandeRevisionBudgetaire validerDemande(
            UUID demandeId, UUID valideurId, String statutChoisi, String commentaire) {
        DemandeRevisionBudgetaire demande = demandeRepository.findByIdForUpdate(demandeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Demande de révision introuvable avec l'ID : " + demandeId));

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Seule une demande soumise peut être décidée.");
        }
        if (valideurId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant du valideur est obligatoire.");
        }
        if (Objects.equals(demande.getDemandeurId(), valideurId)) {
            // Séparation des tâches : le demandeur ne peut pas décider de sa propre demande.
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Vous ne pouvez pas valider ou rejeter votre propre demande de révision.");
        }

        String decision = statutChoisi == null ? "" : statutChoisi.trim().toUpperCase(Locale.ROOT);
        if (!"VALIDE".equals(decision) && !"REJETE".equals(decision)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choisissez le statut Validée ou Rejetée.");
        }

        demande.setStatut(decision);
        demande.setValideurId(valideurId);
        demande.setDateValidation(LocalDateTime.now());
        demande.setCommentaireValidation(commentaire);

        if ("REJETE".equals(decision)) {
            return demandeRepository.save(demande);
        }

        List<LigneRevisionBudgetaire> lignes = ligneRepository.findByDemandeRevisionBudgetaireId(demandeId);
        if (lignes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Une demande sans ligne de mouvement ne peut pas être validée.");
        }

        Map<UUID, BigDecimal> debitsParAllocation = new HashMap<>();
        for (LigneRevisionBudgetaire ligne : lignes) {
            validerLigne(ligne, demande.getAnneeExercice());
            if (ligne.getAllocationSource() != null) {
                debitsParAllocation.merge(
                        ligne.getAllocationSource().getId(), ligne.getMontant(), BigDecimal::add);
            }
        }

        List<UUID> allocationIds = lignes.stream()
                .flatMap(ligne -> java.util.stream.Stream.of(
                        ligne.getAllocationSource() == null ? null : ligne.getAllocationSource().getId(),
                        ligne.getAllocationDestination() == null ? null : ligne.getAllocationDestination().getId()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        Map<UUID, BudgetAllocationStructure> allocationsVerrouillees = new HashMap<>();
        for (UUID allocationId : allocationIds) {
            BudgetAllocationStructure allocation = allocationStructureRepository.findByIdForUpdate(allocationId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Une allocation de la demande est introuvable : " + allocationId));
            allocationsVerrouillees.put(allocationId, allocation);
        }

        debitsParAllocation.forEach((allocationId, debit) -> {
            BudgetAllocationStructure source = allocationsVerrouillees.get(allocationId);
            BigDecimal disponible = valeur(source.getMontantVote())
                    .add(valeur(source.getMontantModifie()))
                    .subtract(valeur(source.getMontantEngage()))
                    .subtract(valeur(source.getMontantLiquide()));
            if (debit.compareTo(disponible) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Crédit disponible insuffisant sur l'allocation source " + allocationId
                                + " : " + disponible + " disponible(s), " + debit + " demandé(s).");
            }
        });

        for (LigneRevisionBudgetaire ligne : lignes) {
            BudgetAllocationStructure source = ligne.getAllocationSource() == null
                    ? null
                    : allocationsVerrouillees.get(ligne.getAllocationSource().getId());
            BudgetAllocationStructure destination =
                    allocationsVerrouillees.get(ligne.getAllocationDestination().getId());

            if (source != null) {
                appliquerMouvement(demande, ligne, source, ligne.getMontant().negate(), valideurId);
            }
            appliquerMouvement(demande, ligne, destination, ligne.getMontant(), valideurId);
        }

        return demandeRepository.save(demande);
    }

    private void validerLigne(LigneRevisionBudgetaire ligne, Integer anneeExercice) {
        if (ligne.getMontant() == null || ligne.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chaque montant de révision doit être positif.");
        }
        if (ligne.getAllocationDestination() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chaque mouvement doit avoir une allocation destination.");
        }
        if ("VIREMENT_INTERNE".equals(ligne.getTypeMouvement())) {
            if (ligne.getAllocationSource() == null
                    || ligne.getAllocationSource().getId().equals(ligne.getAllocationDestination().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Un virement interne doit avoir deux allocations distinctes.");
            }
        } else if ("EXTENSION".equals(ligne.getTypeMouvement())) {
            if (ligne.getAllocationSource() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Une extension budgétaire ne doit pas avoir d'allocation source.");
            }
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Type de mouvement inconnu : " + ligne.getTypeMouvement());
        }

        validerAllocationPourExercice(ligne.getAllocationDestination(), anneeExercice);
        if (ligne.getAllocationSource() != null) {
            validerAllocationPourExercice(ligne.getAllocationSource(), anneeExercice);
        }
    }

    private void validerAllocationPourExercice(BudgetAllocationStructure allocation, Integer anneeExercice) {
        if (allocation.getBudgetAllocationActivite() == null
                || allocation.getBudgetAllocationActivite().getBudgetAllocationOp() == null
                || allocation.getBudgetAllocationActivite().getBudgetAllocationOp().getBudgetAllocationOs() == null
                || allocation.getBudgetAllocationActivite().getBudgetAllocationOp()
                        .getBudgetAllocationOs().getExerciceBudgetaire() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Une allocation de la demande n'est pas rattachée à un exercice valide.");
        }
        ExerciceBudgetaire exercice = allocation.getBudgetAllocationActivite()
                .getBudgetAllocationOp()
                .getBudgetAllocationOs()
                .getExerciceBudgetaire();
        Integer anneeAllocation = exercice.getAnnee();
        if (!Objects.equals(anneeExercice, anneeAllocation)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les allocations de la demande doivent appartenir à l'exercice " + anneeExercice + ".");
        }
        if (STATUT_EXERCICE_CLOTURE.equalsIgnoreCase(exercice.getStatut())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'exercice " + anneeExercice + " est clôturé : aucune révision budgétaire n'est possible.");
        }
    }

    private void appliquerMouvement(
            DemandeRevisionBudgetaire demande,
            LigneRevisionBudgetaire ligne,
            BudgetAllocationStructure allocation,
            BigDecimal variation,
            UUID valideurId) {
        BigDecimal montantModifieAnterieur = valeur(allocation.getMontantModifie());
        BigDecimal montantAnterieur = valeur(allocation.getMontantVote()).add(montantModifieAnterieur);
        BigDecimal nouveauMontant = montantAnterieur.add(variation);
        allocation.setMontantModifie(montantModifieAnterieur.add(variation));
        allocationStructureRepository.save(allocation);

        journalRepository.save(JournalMouvementsBudgetaire.builder()
                .allocationStructure(allocation)
                .anneeExercice(demande.getAnneeExercice())
                .typeMouvement(ligne.getTypeMouvement())
                .montantAnterieur(montantAnterieur)
                .variation(variation)
                .nouveauMontant(nouveauMontant)
                .referenceDocument(demande.getNumeroDemande())
                .auteurId(valideurId)
                .build());
    }

    private BigDecimal valeur(BigDecimal montant) {
        return montant == null ? BigDecimal.ZERO : montant;
    }

    public List<DemandeRevisionBudgetaire> filtrerDemandes(
            Integer anneeExercice, String statut, UUID utilisateurId, boolean peutValider) {
        return demandeRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(visibilite(root, cb, utilisateurId, peutValider));
            if (anneeExercice != null) {
                predicates.add(cb.equal(root.get("anneeExercice"), anneeExercice));
            }
            if (statut != null && !statut.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("statut")), "%" + statut.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    public String exporterEnCsv(Integer anneeExercice, String statut, UUID utilisateurId, boolean peutValider) {
        return exporterEnCsv(filtrerDemandes(anneeExercice, statut, utilisateurId, peutValider));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public String exporterEnCsv(List<DemandeRevisionBudgetaire> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Numéro Demande,Année Exercice,Statut,Motif\n");
        for (DemandeRevisionBudgetaire d : list) {
            sb.append(CsvUtils.ligne(',', d.getId(), d.getNumeroDemande(), d.getAnneeExercice(), d.getStatut(), d.getMotif()));
        }
        return sb.toString();
    }

    public byte[] exporterEnPdf(Integer anneeExercice, String statut, UUID utilisateurId, boolean peutValider) {
        return exporterEnPdf(filtrerDemandes(anneeExercice, statut, utilisateurId, peutValider));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public byte[] exporterEnPdf(List<DemandeRevisionBudgetaire> list) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            doc.add(new com.lowagie.text.Paragraph("Liste des Demandes de Révision Budgétaire", titleFont));
            doc.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);

            table.addCell("Numéro");
            table.addCell("Année");
            table.addCell("Statut");
            table.addCell("Motif");

            for (DemandeRevisionBudgetaire d : list) {
                table.addCell(d.getNumeroDemande() != null ? d.getNumeroDemande() : "");
                table.addCell(d.getAnneeExercice() != null ? d.getAnneeExercice().toString() : "");
                table.addCell(d.getStatut() != null ? d.getStatut() : "");
                table.addCell(d.getMotif() != null ? d.getMotif() : "");
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des demandes de révision", e);
        }
    }

    // --- GESTION DES LIGNES ---

    public List<LigneRevisionBudgetaire> listerLignesParDemande(UUID demandeId) {
        return ligneRepository.findByDemandeRevisionBudgetaireId(demandeId);
    }

    public List<LigneRevisionBudgetaire> filtrerLignes(UUID demandeId, String typeMouvement, UUID allocationSourceId,
                                                       UUID allocationDestinationId, UUID utilisateurId, boolean peutValider) {
        return ligneRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(visibilite(root.get("demandeRevisionBudgetaire"), cb, utilisateurId, peutValider));
            if (demandeId != null) {
                predicates.add(cb.equal(root.get("demandeRevisionBudgetaire").get("id"), demandeId));
            }
            if (typeMouvement != null && !typeMouvement.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("typeMouvement")), "%" + typeMouvement.toLowerCase() + "%"));
            }
            if (allocationSourceId != null) {
                predicates.add(cb.equal(root.get("allocationSource").get("id"), allocationSourceId));
            }
            if (allocationDestinationId != null) {
                predicates.add(cb.equal(root.get("allocationDestination").get("id"), allocationDestinationId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    public String exporterLignesCsv(UUID demandeId, String typeMouvement, UUID allocationSourceId,
                                    UUID allocationDestinationId, UUID utilisateurId, boolean peutValider) {
        List<LigneRevisionBudgetaire> list = filtrerLignes(
                demandeId, typeMouvement, allocationSourceId, allocationDestinationId, utilisateurId, peutValider);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Demande ID,Type Mouvement,Montant,Source ID,Destination ID\n");
        for (LigneRevisionBudgetaire l : list) {
            sb.append(CsvUtils.ligne(',', l.getId(),
                    l.getDemandeRevisionBudgetaire() != null ? l.getDemandeRevisionBudgetaire().getId() : null,
                    l.getTypeMouvement(), l.getMontant(),
                    l.getAllocationSource() != null ? l.getAllocationSource().getId() : null,
                    l.getAllocationDestination() != null ? l.getAllocationDestination().getId() : null));
        }
        return sb.toString();
    }

    public byte[] exporterLignesPdf(UUID demandeId, String typeMouvement, UUID allocationSourceId,
                                    UUID allocationDestinationId, UUID utilisateurId, boolean peutValider) {
        List<LigneRevisionBudgetaire> list = filtrerLignes(
                demandeId, typeMouvement, allocationSourceId, allocationDestinationId, utilisateurId, peutValider);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            doc.add(new com.lowagie.text.Paragraph("Détail des Lignes de Révision Budgétaire", titleFont));
            doc.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);

            table.addCell("Type Mouvement");
            table.addCell("Montant");
            table.addCell("Source");
            table.addCell("Destination");

            for (LigneRevisionBudgetaire l : list) {
                table.addCell(l.getTypeMouvement() != null ? l.getTypeMouvement() : "");
                table.addCell(l.getMontant() != null ? l.getMontant().toString() : "");
                table.addCell(l.getAllocationSource() != null ? l.getAllocationSource().getId().toString() : "-");
                table.addCell(l.getAllocationDestination() != null ? l.getAllocationDestination().getId().toString() : "-");
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des lignes de révision", e);
        }
    }
}