package com.budget.backend.budget.service;

import com.budget.backend.budget.dto.LigneRevisionRequestDto;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.entity.DemandeRevisionBudgetaire;
import com.budget.backend.budget.entity.JournalMouvementsBudgetaire;
import com.budget.backend.budget.entity.LigneRevisionBudgetaire;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.budget.repository.DemandeRevisionBudgetaireRepository;
import com.budget.backend.budget.repository.JournalMouvementsBudgetaireRepository;
import com.budget.backend.budget.repository.LigneRevisionBudgetaireRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RevisionBudgetaireService {

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

    public Optional<DemandeRevisionBudgetaire> trouverParId(UUID id) {
        return demandeRepository.findById(id);
    }

    @Transactional
    public DemandeRevisionBudgetaire creerDemandeAvecLignes(DemandeRevisionBudgetaire demande, List<LigneRevisionRequestDto> lignesDto) {
        demande.setStatut("BROUILLON");
        DemandeRevisionBudgetaire demandeSauvegardee = demandeRepository.save(demande);

        if (lignesDto != null && !lignesDto.isEmpty()) {
            for (LigneRevisionRequestDto ligneDto : lignesDto) {
                BudgetAllocationStructure source = null;
                if (ligneDto.getAllocationSourceId() != null) {
                    source = allocationStructureRepository.findById(ligneDto.getAllocationSourceId())
                            .orElseThrow(() -> new RuntimeException("Allocation source introuvable avec l'ID : " + ligneDto.getAllocationSourceId()));
                }

                BudgetAllocationStructure destination = null;
                if (ligneDto.getAllocationDestinationId() != null) {
                    destination = allocationStructureRepository.findById(ligneDto.getAllocationDestinationId())
                            .orElseThrow(() -> new RuntimeException("Allocation destination introuvable avec l'ID : " + ligneDto.getAllocationDestinationId()));
                }

                LigneRevisionBudgetaire ligne = LigneRevisionBudgetaire.builder()
                        .demandeRevisionBudgetaire(demandeSauvegardee)
                        .typeMouvement(ligneDto.getTypeMouvement())
                        .montant(ligneDto.getMontant())
                        .allocationSource(source)
                        .allocationDestination(destination)
                        .build();

                ligneRepository.save(ligne);
            }
        }

        return demandeSauvegardee;
    }

    @Transactional
    public DemandeRevisionBudgetaire validerDemande(UUID demandeId, UUID valideurId, String commentaire) {
        DemandeRevisionBudgetaire demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande de révision introuvable avec l'ID : " + demandeId));

        demande.setStatut("VALIDE");
        demande.setValideurId(valideurId);
        demande.setDateValidation(LocalDateTime.now());
        demande.setCommentaireValidation(commentaire);

        List<LigneRevisionBudgetaire> lignes = ligneRepository.findByDemandeRevisionBudgetaireId(demandeId);
        for (LigneRevisionBudgetaire ligne : lignes) {
            // Mouvement source (-)
            if (ligne.getAllocationSource() != null) {
                BudgetAllocationStructure source = ligne.getAllocationSource();
                BigDecimal montantAnterieur = source.getMontantModifie() != null ? source.getMontantModifie() : BigDecimal.ZERO;
                BigDecimal nouveauMontant = montantAnterieur.subtract(ligne.getMontant());
                source.setMontantModifie(nouveauMontant);
                allocationStructureRepository.save(source);

                JournalMouvementsBudgetaire mouvementSource = JournalMouvementsBudgetaire.builder()
                        .allocationStructure(source)
                        .anneeExercice(demande.getAnneeExercice())
                        .typeMouvement(ligne.getTypeMouvement())
                        .montantAnterieur(montantAnterieur)
                        .variation(ligne.getMontant().negate())
                        .nouveauMontant(nouveauMontant)
                        .referenceDocument(demande.getNumeroDemande())
                        .auteurId(demande.getDemandeurId())
                        .build();
                journalRepository.save(mouvementSource);
            }

            // Mouvement destination (+)
            if (ligne.getAllocationDestination() != null) {
                BudgetAllocationStructure destination = ligne.getAllocationDestination();
                BigDecimal montantAnterieur = destination.getMontantModifie() != null ? destination.getMontantModifie() : BigDecimal.ZERO;
                BigDecimal nouveauMontant = montantAnterieur.add(ligne.getMontant());
                destination.setMontantModifie(nouveauMontant);
                allocationStructureRepository.save(destination);

                JournalMouvementsBudgetaire mouvementDest = JournalMouvementsBudgetaire.builder()
                        .allocationStructure(destination)
                        .anneeExercice(demande.getAnneeExercice())
                        .typeMouvement(ligne.getTypeMouvement())
                        .montantAnterieur(montantAnterieur)
                        .variation(ligne.getMontant())
                        .nouveauMontant(nouveauMontant)
                        .referenceDocument(demande.getNumeroDemande())
                        .auteurId(demande.getDemandeurId())
                        .build();
                journalRepository.save(mouvementDest);
            }
        }

        return demandeRepository.save(demande);
    }

    public List<DemandeRevisionBudgetaire> filtrerDemandes(Integer anneeExercice, String statut) {
        return demandeRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (anneeExercice != null) {
                predicates.add(cb.equal(root.get("anneeExercice"), anneeExercice));
            }
            if (statut != null && !statut.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("statut")), "%" + statut.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    public String exporterEnCsv(Integer anneeExercice, String statut) {
        List<DemandeRevisionBudgetaire> list = filtrerDemandes(anneeExercice, statut);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Numéro Demande,Année Exercice,Statut,Motif\n");
        for (DemandeRevisionBudgetaire d : list) {
            sb.append(d.getId()).append(",")
                    .append("\"").append(d.getNumeroDemande()).append("\",")
                    .append(d.getAnneeExercice()).append(",")
                    .append("\"").append(d.getStatut()).append("\",")
                    .append("\"").append(d.getMotif() != null ? d.getMotif().replace("\"", "\"\"") : "").append("\"\n");
        }
        return sb.toString();
    }

    public byte[] exporterEnPdf(Integer anneeExercice, String statut) {
        List<DemandeRevisionBudgetaire> list = filtrerDemandes(anneeExercice, statut);
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

    public List<LigneRevisionBudgetaire> filtrerLignes(UUID demandeId, String typeMouvement, UUID allocationSourceId, UUID allocationDestinationId) {
        return ligneRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
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

    public String exporterLignesCsv(UUID demandeId, String typeMouvement, UUID allocationSourceId, UUID allocationDestinationId) {
        List<LigneRevisionBudgetaire> list = filtrerLignes(demandeId, typeMouvement, allocationSourceId, allocationDestinationId);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Demande ID,Type Mouvement,Montant,Source ID,Destination ID\n");
        for (LigneRevisionBudgetaire l : list) {
            sb.append(l.getId()).append(",")
                    .append(l.getDemandeRevisionBudgetaire() != null ? l.getDemandeRevisionBudgetaire().getId() : "").append(",")
                    .append("\"").append(l.getTypeMouvement()).append("\",")
                    .append(l.getMontant()).append(",")
                    .append(l.getAllocationSource() != null ? l.getAllocationSource().getId() : "").append(",")
                    .append(l.getAllocationDestination() != null ? l.getAllocationDestination().getId() : "").append("\n");
        }
        return sb.toString();
    }

    public byte[] exporterLignesPdf(UUID demandeId, String typeMouvement, UUID allocationSourceId, UUID allocationDestinationId) {
        List<LigneRevisionBudgetaire> list = filtrerLignes(demandeId, typeMouvement, allocationSourceId, allocationDestinationId);
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