package com.budget.backend.workflow.service;

import com.budget.backend.achat.entity.HistoriqueValidation;
import com.budget.backend.achat.repository.HistoriqueValidationRepository;
import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.workflow.WorkflowException;
import com.budget.backend.workflow.dto.*;
import com.budget.backend.workflow.entity.WorkflowDefinition;
import com.budget.backend.workflow.entity.WorkflowEtape;
import com.budget.backend.workflow.entity.WorkflowInstance;
import com.budget.backend.workflow.repository.WorkflowDefinitionRepository;
import com.budget.backend.workflow.repository.WorkflowEtapeRepository;
import com.budget.backend.workflow.repository.WorkflowInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkflowService {

    @Autowired
    private WorkflowDefinitionRepository definitionRepository;

    @Autowired
    private WorkflowEtapeRepository etapeRepository;

    @Autowired
    private WorkflowInstanceRepository instanceRepository;

    @Autowired
    private HistoriqueValidationRepository historiqueValidationRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private com.budget.backend.achat.repository.DemandeAchatRepository demandeAchatRepository;

    @Autowired
    private com.budget.backend.abonnement.repository.AbonnementRepository abonnementRepository;

    @Autowired
    private com.budget.backend.stock.repository.DemandeMagasinRepository demandeMagasinRepository;

    // =========================================================================
    // 1. GESTION DES MODÈLES DE WORKFLOW (workflow_definitions)
    // =========================================================================

    @Transactional(readOnly = true)
    public PageResponseDto<WorkflowDefinitionResponseDto> filtrerDefinitions(
            String typeEntite, Boolean actif, String recherche, Pageable pageable) {
        return PageResponseDto.from(definitionRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (typeEntite != null && !typeEntite.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("typeEntite")), typeEntite.trim().toUpperCase()));
            }
            if (actif != null) {
                predicates.add(cb.equal(root.get("actif"), actif));
            }
            if (recherche != null && !recherche.isBlank()) {
                String term = "%" + recherche.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), term),
                        cb.like(cb.lower(root.get("libelle")), term),
                        cb.like(cb.lower(root.get("description")), term)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), WorkflowDefinitionResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowDefinitionResponseDto> trouverDefinitionParId(UUID id) {
        return definitionRepository.findById(id).map(WorkflowDefinitionResponseDto::from);
    }

    @Transactional
    public WorkflowDefinitionResponseDto creerDefinition(WorkflowDefinitionRequestDto dto) {
        String code = dto.getCode().trim().toUpperCase();
        if (definitionRepository.existsByCode(code)) {
            throw new WorkflowException("Un modèle de workflow avec le code " + code + " existe déjà.");
        }

        WorkflowDefinition def = WorkflowDefinition.builder()
                .code(code)
                .libelle(dto.getLibelle().trim())
                .description(dto.getDescription())
                .typeEntite(dto.getTypeEntite().trim().toUpperCase())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .etapes(new ArrayList<>())
                .build();

        def = definitionRepository.save(def);
        return WorkflowDefinitionResponseDto.from(def);
    }

    @Transactional
    public WorkflowDefinitionResponseDto modifierDefinition(UUID id, WorkflowDefinitionRequestDto dto) {
        WorkflowDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Modèle de workflow introuvable avec l'ID : " + id));

        def.setLibelle(dto.getLibelle().trim());
        def.setDescription(dto.getDescription());
        def.setTypeEntite(dto.getTypeEntite().trim().toUpperCase());
        if (dto.getActif() != null) {
            def.setActif(dto.getActif());
        }

        def = definitionRepository.save(def);
        return WorkflowDefinitionResponseDto.from(def);
    }

    @Transactional
    public void supprimerDefinition(UUID id) {
        WorkflowDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Modèle de workflow introuvable avec l'ID : " + id));
        definitionRepository.delete(def);
    }

    // =========================================================================
    // 2. GESTION DES ÉTAPES DU WORKFLOW (workflow_etapes)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<WorkflowEtapeDto> listerEtapes(UUID workflowId) {
        return etapeRepository.findByWorkflow_IdOrderByNumEtapeAsc(workflowId).stream()
                .map(WorkflowEtapeDto::from)
                .toList();
    }

    @Transactional
    public WorkflowEtapeDto ajouterOuModifierEtape(UUID workflowId, WorkflowEtapeDto dto) {
        WorkflowDefinition def = definitionRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowException("Workflow introuvable avec l'ID : " + workflowId));

        WorkflowEtape etape;
        if (dto.getId() != null) {
            etape = etapeRepository.findById(dto.getId())
                    .orElseThrow(() -> new WorkflowException("Étape introuvable avec l'ID : " + dto.getId()));
        } else {
            etape = new WorkflowEtape();
            etape.setWorkflow(def);
        }

        etape.setNumEtape(dto.getNumEtape());
        etape.setCodeEtape(dto.getCodeEtape().trim().toUpperCase());
        etape.setLibelle(dto.getLibelle().trim());
        etape.setPermissionRequise(dto.getPermissionRequise());
        etape.setRoleRequis(dto.getRoleRequis());
        etape.setMontantMinimum(dto.getMontantMinimum() != null ? dto.getMontantMinimum() : BigDecimal.ZERO);
        etape.setMontantMaximum(dto.getMontantMaximum());
        etape.setEstEtapeFinale(Boolean.TRUE.equals(dto.getEstEtapeFinale()));
        etape.setEstEtapeRejet(Boolean.TRUE.equals(dto.getEstEtapeRejet()));

        if (dto.getEtapeSuivanteSuccesId() != null) {
            WorkflowEtape suivante = etapeRepository.findById(dto.getEtapeSuivanteSuccesId()).orElse(null);
            etape.setEtapeSuivanteSucces(suivante);
        }
        if (dto.getEtapeSuivanteRejetId() != null) {
            WorkflowEtape rejet = etapeRepository.findById(dto.getEtapeSuivanteRejetId()).orElse(null);
            etape.setEtapeSuivanteRejet(rejet);
        }

        etape = etapeRepository.save(etape);
        return WorkflowEtapeDto.from(etape);
    }

    @Transactional
    public void supprimerEtape(UUID workflowId, UUID etapeId) {
        WorkflowEtape etape = etapeRepository.findById(etapeId)
                .orElseThrow(() -> new WorkflowException("Étape introuvable avec l'ID : " + etapeId));
        etapeRepository.delete(etape);
    }

    // =========================================================================
    // 3. MOTEUR D'INSTANCIATION & EXÉCUTION (workflow_instances)
    // =========================================================================

    @Transactional
    public WorkflowInstanceResponseDto demarrerWorkflow(
            String typeEntite, UUID dossierId, String referenceDossier, String objet, BigDecimal montantTotal, Utilisateur demandeur) {
        
        // Trouver le workflow actif pour ce type d'entité
        WorkflowDefinition def = definitionRepository.findByTypeEntiteAndActifTrue(typeEntite.trim().toUpperCase())
                .orElseThrow(() -> new WorkflowException("Aucun workflow actif configuré pour le type : " + typeEntite));

        WorkflowEtape premiereEtape = etapeRepository.findFirstByWorkflow_IdOrderByNumEtapeAsc(def.getId())
                .orElseThrow(() -> new WorkflowException("Le workflow " + def.getCode() + " ne contient aucune étape configurée."));

        if (demandeur == null) {
            demandeur = currentUserService.utilisateurConnecte();
        }

        WorkflowInstance instance = WorkflowInstance.builder()
                .workflow(def)
                .dossierId(dossierId)
                .typeEntite(typeEntite.trim().toUpperCase())
                .referenceDossier(referenceDossier)
                .objet(objet)
                .montantTotal(montantTotal != null ? montantTotal : BigDecimal.ZERO)
                .demandeur(demandeur)
                .etapeCourante(premiereEtape)
                .statutActuel(WorkflowInstance.EN_ATTENTE_VALIDATION)
                .build();

        instance = instanceRepository.save(instance);
        return WorkflowInstanceResponseDto.from(instance);
    }

    @Transactional
    public Optional<WorkflowInstanceResponseDto> demarrerWorkflowSiActif(
            String typeEntite, UUID dossierId, String referenceDossier, String objet, BigDecimal montantTotal, Utilisateur demandeur) {
        
        Optional<WorkflowDefinition> defOpt = definitionRepository.findByTypeEntiteAndActifTrue(typeEntite.trim().toUpperCase());
        if (defOpt.isEmpty()) {
            return Optional.empty();
        }
        WorkflowDefinition def = defOpt.get();
        Optional<WorkflowEtape> etapeOpt = etapeRepository.findFirstByWorkflow_IdOrderByNumEtapeAsc(def.getId());
        if (etapeOpt.isEmpty()) {
            return Optional.empty();
        }

        if (demandeur == null) {
            demandeur = currentUserService.utilisateurConnecte();
        }

        Optional<WorkflowInstance> existante = instanceRepository.findByDossierId(dossierId);
        WorkflowInstance instance;
        if (existante.isPresent()) {
            instance = existante.get();
            instance.setEtapeCourante(etapeOpt.get());
            instance.setStatutActuel(WorkflowInstance.EN_ATTENTE_VALIDATION);
            instance.setMontantTotal(montantTotal != null ? montantTotal : BigDecimal.ZERO);
            instance.setObjet(objet);
        } else {
            instance = WorkflowInstance.builder()
                    .workflow(def)
                    .dossierId(dossierId)
                    .typeEntite(typeEntite.trim().toUpperCase())
                    .referenceDossier(referenceDossier)
                    .objet(objet)
                    .montantTotal(montantTotal != null ? montantTotal : BigDecimal.ZERO)
                    .demandeur(demandeur)
                    .etapeCourante(etapeOpt.get())
                    .statutActuel(WorkflowInstance.EN_ATTENTE_VALIDATION)
                    .build();
        }

        instance = instanceRepository.save(instance);
        return Optional.of(WorkflowInstanceResponseDto.from(instance));
    }

    @Transactional(readOnly = true)
    public PageResponseDto<WorkflowInstanceResponseDto> filtrerInstances(
            String statut, String typeEntite, String recherche, Pageable pageable) {
        return PageResponseDto.from(instanceRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (statut != null && !statut.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("statutActuel")), statut.trim().toUpperCase()));
            }
            if (typeEntite != null && !typeEntite.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("typeEntite")), typeEntite.trim().toUpperCase()));
            }
            if (recherche != null && !recherche.isBlank()) {
                String term = "%" + recherche.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("referenceDossier")), term),
                        cb.like(cb.lower(root.get("objet")), term)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), WorkflowInstanceResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowInstanceResponseDto> trouverInstanceParId(UUID id) {
        return instanceRepository.findById(id).map(WorkflowInstanceResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowInstanceResponseDto> trouverInstanceParDossierId(UUID dossierId) {
        return instanceRepository.findByDossierId(dossierId).map(WorkflowInstanceResponseDto::from);
    }

    @Transactional(readOnly = true)
    public List<com.budget.backend.achat.dto.HistoriqueValidationResponseDto> trouverHistoriqueInstance(UUID instanceId) {
        return historiqueValidationRepository.findByInstanceIdOrderByDateDecisionAsc(instanceId)
                .stream()
                .map(com.budget.backend.achat.dto.HistoriqueValidationResponseDto::from)
                .toList();
    }

    @Transactional
    public WorkflowInstanceResponseDto traiterDecision(UUID instanceId, WorkflowDecisionRequestDto dto) {
        WorkflowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new WorkflowException("Instance de workflow introuvable avec l'ID : " + instanceId));

        if (!WorkflowInstance.EN_ATTENTE_VALIDATION.equals(instance.getStatutActuel())) {
            throw new WorkflowException("Ce dossier n'est plus en attente de validation (Statut : " + instance.getStatutActuel() + ").");
        }

        WorkflowEtape etapeCourante = instance.getEtapeCourante();
        if (etapeCourante == null) {
            throw new WorkflowException("Aucune étape active définie pour cette instance de workflow.");
        }

        Utilisateur valideur = currentUserService.utilisateurConnecte();
        String decision = dto.getDecision().trim().toUpperCase();

        if ("VALIDER".equals(decision) || "APPROUVER".equals(decision)) {
            // Passer à l'étape suivante ou marquer comme approuvé
            WorkflowEtape prochaine = etapeCourante.getEtapeSuivanteSucces();

            // Vérification de seuil : si la prochaine étape a un seuil minimum non atteint, on peut sauter vers la suivante
            while (prochaine != null && prochaine.getMontantMinimum() != null 
                    && instance.getMontantTotal().compareTo(prochaine.getMontantMinimum()) < 0) {
                prochaine = prochaine.getEtapeSuivanteSucces();
            }

            if (prochaine == null || Boolean.TRUE.equals(etapeCourante.getEstEtapeFinale())) {
                instance.setEtapeCourante(null);
                instance.setStatutActuel(WorkflowInstance.APPROUVE);
            } else {
                instance.setEtapeCourante(prochaine);
            }

            // Enregistrer l'audit
            historiqueValidationRepository.save(HistoriqueValidation.builder()
                    .demandeAchat(null) // lié via instance_id
                    .instanceId(instance.getId())
                    .etape(etapeCourante.getCodeEtape())
                    .valideur(valideur)
                    .decision("APPROUVE")
                    .commentaire(dto.getCommentaire())
                    .build());

        } else if ("REJETER".equals(decision)) {
            WorkflowEtape rejet = etapeCourante.getEtapeSuivanteRejet();
            if (rejet != null) {
                instance.setEtapeCourante(rejet);
            } else {
                instance.setEtapeCourante(null);
                instance.setStatutActuel(WorkflowInstance.REJETE);
            }

            // Enregistrer l'audit
            historiqueValidationRepository.save(HistoriqueValidation.builder()
                    .demandeAchat(null)
                    .instanceId(instance.getId())
                    .etape(etapeCourante.getCodeEtape())
                    .valideur(valideur)
                    .decision("REJETE")
                    .commentaire(dto.getCommentaire())
                    .build());
        } else {
            throw new WorkflowException("Décision non reconnue : " + decision + ". Valeurs acceptées : VALIDER, REJETER.");
        }

        instance = instanceRepository.save(instance);

        // Synchroniser le statut avec le dossier métier sous-jacent (DA, Abonnement, Stock)
        synchroniserStatutEntiteMetier(instance);

        return WorkflowInstanceResponseDto.from(instance);
    }

    private void synchroniserStatutEntiteMetier(WorkflowInstance instance) {
        if ("DEMANDE_ACHAT".equalsIgnoreCase(instance.getTypeEntite())) {
            demandeAchatRepository.findById(instance.getDossierId()).ifPresent(da -> {
                if (WorkflowInstance.APPROUVE.equals(instance.getStatutActuel())) {
                    da.setStatut(com.budget.backend.achat.entity.DemandeAchat.APPROUVE);
                    da.setEtapeCourante("APPROUVE");
                } else if (WorkflowInstance.REJETE.equals(instance.getStatutActuel())) {
                    da.setStatut(com.budget.backend.achat.entity.DemandeAchat.REJETE);
                    da.setEtapeCourante("REJETE");
                } else {
                    da.setStatut(com.budget.backend.achat.entity.DemandeAchat.EN_COURS_VALIDATION);
                    if (instance.getEtapeCourante() != null) {
                        da.setEtapeCourante(instance.getEtapeCourante().getCodeEtape());
                    }
                }
                demandeAchatRepository.save(da);
            });
        } else if ("CONTRAT_ABONNEMENT".equalsIgnoreCase(instance.getTypeEntite())) {
            abonnementRepository.findById(instance.getDossierId()).ifPresent(abn -> {
                if (WorkflowInstance.APPROUVE.equals(instance.getStatutActuel())) {
                    abn.setStatut(com.budget.backend.abonnement.entity.Abonnement.ACTIF);
                } else if (WorkflowInstance.REJETE.equals(instance.getStatutActuel())) {
                    abn.setStatut(com.budget.backend.abonnement.entity.Abonnement.SUSPENDU);
                }
                abonnementRepository.save(abn);
            });
        } else if ("DEMANDE_STOCK".equalsIgnoreCase(instance.getTypeEntite())) {
            demandeMagasinRepository.findById(instance.getDossierId()).ifPresent(dm -> {
                if (WorkflowInstance.APPROUVE.equals(instance.getStatutActuel())) {
                    dm.setStatut(com.budget.backend.stock.entity.DemandeMagasin.VALIDE_N1);
                } else if (WorkflowInstance.REJETE.equals(instance.getStatutActuel())) {
                    dm.setStatut(com.budget.backend.stock.entity.DemandeMagasin.REJETEE);
                }
                demandeMagasinRepository.save(dm);
            });
        }
    }

    @Transactional(readOnly = true)
    public List<WorkflowInstanceResponseDto> trouverMesValidationsEnAttente() {
        Utilisateur user = currentUserService.utilisateurConnecte();
        boolean estAdmin = user.getRole() != null && "ADMIN_SYSTEME".equals(user.getRole().getCode());

        List<WorkflowInstance> enAttente = instanceRepository.findAll((root, query, cb) ->
                cb.equal(root.get("statutActuel"), WorkflowInstance.EN_ATTENTE_VALIDATION));

        if (estAdmin) {
            return enAttente.stream().map(WorkflowInstanceResponseDto::from).toList();
        }

        String roleUser = user.getRole() != null ? user.getRole().getCode() : "";
        return enAttente.stream()
                .filter(inst -> {
                    WorkflowEtape etape = inst.getEtapeCourante();
                    if (etape == null) return false;
                    if (etape.getRoleRequis() != null && !etape.getRoleRequis().isBlank()) {
                        return etape.getRoleRequis().equalsIgnoreCase(roleUser);
                    }
                    return true;
                })
                .map(WorkflowInstanceResponseDto::from)
                .toList();
    }
}
