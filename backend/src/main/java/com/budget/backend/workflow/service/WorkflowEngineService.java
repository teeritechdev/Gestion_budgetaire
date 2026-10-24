package com.budget.backend.workflow.service;

import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.stock.service.AuditHelper;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.workflow.WorkflowException;
import com.budget.backend.workflow.api.DossierMontantProvider;
import com.budget.backend.workflow.api.WorkflowEngineApi;
import com.budget.backend.workflow.api.WorkflowTransitionEvent;
import com.budget.backend.workflow.dto.WorkflowExecutionActionDto;
import com.budget.backend.workflow.dto.WorkflowInstanceResponseDto;
import com.budget.backend.workflow.entity.WorkflowDefinition;
import com.budget.backend.workflow.entity.WorkflowEtape;
import com.budget.backend.workflow.entity.WorkflowInstance;
import com.budget.backend.workflow.repository.WorkflowDefinitionRepository;
import com.budget.backend.workflow.repository.WorkflowInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Moteur de workflow : fait avancer un dossier dans le graphe d'étapes d'une définition.
 * Règles du PDF : l'utilisateur doit posséder la permission de l'étape ; l'étape suivante dépend du résultat
 * (succès ou rejet) ; une étape n'est active que si le montant du dossier est dans sa plage.
 */
@Service
public class WorkflowEngineService implements WorkflowEngineApi {

    @Autowired
    private WorkflowDefinitionRepository definitionRepository;
    @Autowired
    private WorkflowInstanceRepository instanceRepository;
    @Autowired(required = false)
    private List<DossierMontantProvider> montantProviders = new ArrayList<>();
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private CurrentUserService currentUserService;
    @Autowired
    private AuditHelper auditHelper;

    // --- WorkflowEngineApi (appelée par les modules métier) ---

    @Override
    @Transactional
    public EtatWorkflow demarrer(String workflowCode, UUID dossierId, BigDecimal montant) {
        WorkflowDefinition definition = definitionRepository.findByCode(workflowCode)
                .orElseThrow(() -> new WorkflowException("Workflow inconnu : " + workflowCode));
        if (!Boolean.TRUE.equals(definition.getActif())) {
            throw new WorkflowException("Le workflow " + workflowCode + " est désactivé.");
        }
        if (instanceRepository.existsByWorkflowCodeAndDossierId(workflowCode, dossierId)) {
            throw new WorkflowException("Ce dossier est déjà engagé dans le workflow " + workflowCode + ".");
        }
        // Première étape : la plus petite numérotée parmi celles qui s'appliquent au montant du dossier
        WorkflowEtape premiere = definition.getEtapes().stream()
                .filter(e -> e.estApplicable(montant))
                .min(Comparator.comparing(WorkflowEtape::getNumEtape).thenComparing(WorkflowEtape::getCodeEtape))
                .orElseThrow(() -> new WorkflowException("Aucune étape du workflow " + workflowCode
                        + " ne s'applique au montant " + montant + "."));
        WorkflowInstance instance = instanceRepository.saveAndFlush(WorkflowInstance.builder()
                .workflow(definition)
                .dossierId(dossierId)
                .etapeCourante(premiere)
                .statutActuel(STATUT_EN_ATTENTE)
                .build());
        return etat(instance);
    }

    @Override
    @Transactional
    public EtatWorkflow prochaineEtape(String workflowCode, UUID dossierId, BigDecimal montant, boolean approuve, Utilisateur utilisateur) {
        WorkflowInstance instance = instanceRepository.findForUpdate(workflowCode, dossierId)
                .orElseThrow(() -> new WorkflowException("Aucun suivi de workflow " + workflowCode + " pour le dossier " + dossierId + "."));
        return avancer(instance, montant, approuve, utilisateur);
    }

    // --- API REST (POST /workflows/instances/execute) ---

    /** Décision prise directement sur le moteur : le module propriétaire du dossier applique ses effets via l'événement. */
    @Transactional
    public WorkflowInstanceResponseDto executer(WorkflowExecutionActionDto dto) {
        boolean approuve = parseAction(dto.getAction());
        Utilisateur utilisateur = currentUserService.utilisateurConnecte();

        List<WorkflowInstance> enCours = instanceRepository.findByDossierIdOrderByCreatedAtDesc(dto.getDossierId()).stream()
                .filter(i -> i.getEtapeCourante() != null).toList();
        if (enCours.isEmpty()) {
            throw new WorkflowException("Aucun circuit de validation en cours pour le dossier " + dto.getDossierId() + ".");
        }
        if (enCours.size() > 1) {
            throw new WorkflowException("Plusieurs circuits sont en cours pour le dossier " + dto.getDossierId() + " : ambigu.");
        }
        String code = enCours.get(0).getWorkflow().getCode();
        WorkflowInstance instance = instanceRepository.findForUpdate(code, dto.getDossierId()).orElseThrow();
        BigDecimal montant = montants(code, dto.getDossierId());

        String etapeAvant = instance.getEtapeCourante() != null ? instance.getEtapeCourante().getCodeEtape() : null;
        String statutAvant = instance.getStatutActuel();
        EtatWorkflow etat = avancer(instance, montant, approuve, utilisateur);

        eventPublisher.publishEvent(new WorkflowTransitionEvent(code, dto.getDossierId(), etapeAvant, approuve, etat,
                montant, utilisateur, dto.getCommentaire()));
        auditHelper.tracer(utilisateur, AuditHelper.VALIDATION, "workflow_instances", instance.getId(),
                AuditHelper.valeurs("etapeCourante", etapeAvant, "statutActuel", statutAvant),
                AuditHelper.valeurs("etapeCourante", etat.etapeCourante(), "statutActuel", etat.statut(),
                        "action", approuve ? "APPROVE" : "REJECT", "commentaire", dto.getCommentaire()));
        return WorkflowInstanceResponseDto.from(instance);
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowInstanceResponseDto> trouverParDossier(UUID dossierId) {
        return instanceRepository.findByDossierIdOrderByCreatedAtDesc(dossierId).stream().findFirst()
                .map(WorkflowInstanceResponseDto::from);
    }

    // --- Cœur : transition d'une instance ---

    private EtatWorkflow avancer(WorkflowInstance instance, BigDecimal montant, boolean approuve, Utilisateur utilisateur) {
        WorkflowEtape courante = instance.getEtapeCourante();
        if (courante == null) {
            throw new WorkflowException("Le circuit de ce dossier est terminé (statut " + instance.getStatutActuel() + ").");
        }
        if (!possedePermission(utilisateur, courante.getPermissionRequise())) {
            throw new WorkflowException("L'étape " + courante.getCodeEtape() + " (" + courante.getLibelle()
                    + ") exige la permission '" + courante.getPermissionRequise() + "'.");
        }
        // Le dossier est dans le circuit d'exception s'il a déjà été rejeté et attend encore une étape
        boolean enException = WorkflowEngineApi.STATUT_REJETE.equals(instance.getStatutActuel());

        if (approuve) {
            WorkflowEtape suivante = etapeSuivanteApplicable(courante, montant);
            if (suivante == null) {
                instance.setEtapeCourante(null);
                instance.setStatutActuel(enException ? STATUT_ANNULE : STATUT_APPROUVE);
            } else {
                instance.setEtapeCourante(suivante);
                instance.setStatutActuel(enException ? STATUT_REJETE : STATUT_EN_ATTENTE);
            }
        } else {
            // Branche de rejet : étape d'exception (annulation, libération des crédits...) ou fin de circuit
            WorkflowEtape rejet = courante.getEtapeSuivanteRejet();
            instance.setEtapeCourante(rejet);
            instance.setStatutActuel(STATUT_REJETE);
        }
        return etat(instanceRepository.saveAndFlush(instance));
    }

    /** Suit les renvois de succès en sautant les étapes dont la plage de montants ne couvre pas le dossier. */
    private WorkflowEtape etapeSuivanteApplicable(WorkflowEtape courante, BigDecimal montant) {
        Set<UUID> vues = new HashSet<>();
        vues.add(courante.getId());
        WorkflowEtape cible = courante.getEtapeSuivanteSucces();
        while (cible != null && !cible.estApplicable(montant)) {
            if (!vues.add(cible.getId())) {
                throw new WorkflowException("Boucle détectée dans le circuit à l'étape " + cible.getCodeEtape() + ".");
            }
            cible = cible.getEtapeSuivanteSucces();
        }
        return cible;
    }

    /** Comparaison insensible à la casse : ENGAGEMENT_VALIDER_DFI (PDF) comme demande_achat.viser (projet). */
    private boolean possedePermission(Utilisateur u, String permissionRequise) {
        return u != null && u.getRole() != null && u.getRole().getPermissions() != null
                && u.getRole().getPermissions().stream().anyMatch(p -> p.getCode().equalsIgnoreCase(permissionRequise));
    }

    private EtatWorkflow etat(WorkflowInstance i) {
        return new EtatWorkflow(i.getEtapeCourante() != null ? i.getEtapeCourante().getCodeEtape() : null, i.getStatutActuel());
    }

    private boolean parseAction(String action) {
        String a = action == null ? "" : action.trim().toUpperCase();
        return switch (a) {
            case "APPROVE" -> true;
            case "REJECT" -> false;
            default -> throw new WorkflowException("Action invalide : '" + action + "' (attendu : APPROVE ou REJECT).");
        };
    }

    private BigDecimal montants(String workflowCode, UUID dossierId) {
        return montantProviders.stream()
                .filter(p -> p.supporte(workflowCode))
                .map(p -> p.montant(dossierId))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new WorkflowException("Impossible de déterminer le montant du dossier " + dossierId
                        + " pour le workflow " + workflowCode + "."));
    }
}
