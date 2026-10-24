package com.budget.backend.workflow.service;

import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import org.springframework.data.domain.Pageable;
import com.budget.backend.workflow.WorkflowException;
import com.budget.backend.workflow.dto.WorkflowDefinitionRequestDto;
import com.budget.backend.workflow.dto.WorkflowDefinitionResponseDto;
import com.budget.backend.workflow.dto.WorkflowEtapeRequestDto;
import com.budget.backend.workflow.entity.WorkflowDefinition;
import com.budget.backend.workflow.entity.WorkflowEtape;
import com.budget.backend.workflow.repository.WorkflowDefinitionRepository;
import com.budget.backend.workflow.repository.WorkflowEtapeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** Catalogue des processus : définition d'un circuit (étapes, permissions, plages de montants, renvois). */
@Service
public class WorkflowDefinitionService {

    @Autowired
    private WorkflowDefinitionRepository repository;
    @Autowired
    private WorkflowEtapeRepository etapeRepository;

    @Transactional(readOnly = true)
    public PageResponseDto<WorkflowDefinitionResponseDto> filtrer(String code, Boolean actif, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (code != null && !code.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%"));
            }
            if (actif != null) {
                predicates.add(cb.equal(root.get("actif"), actif));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), WorkflowDefinitionResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowDefinitionResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(WorkflowDefinitionResponseDto::from);
    }

    @Transactional
    public WorkflowDefinitionResponseDto creer(WorkflowDefinitionRequestDto dto) {
        if (repository.existsByCode(dto.getCode())) {
            throw new WorkflowException("Un workflow avec ce code existe déjà : " + dto.getCode());
        }
        verifierEtapes(dto.getEtapes());

        WorkflowDefinition definition = WorkflowDefinition.builder()
                .code(dto.getCode())
                .libelle(dto.getLibelle())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();
        Map<String, WorkflowEtape> parCode = new LinkedHashMap<>();
        for (WorkflowEtapeRequestDto e : dto.getEtapes()) {
            WorkflowEtape etape = WorkflowEtape.builder()
                    .workflow(definition)
                    .numEtape(e.getNumEtape())
                    .codeEtape(e.getCodeEtape())
                    .libelle(e.getLibelle())
                    .permissionRequise(e.getPermissionRequise())
                    .montantMinimum(e.getMontantMinimum())
                    .montantMaximum(e.getMontantMaximum())
                    .build();
            definition.getEtapes().add(etape);
            parCode.put(e.getCodeEtape(), etape);
        }
        // Les renvois entre étapes ne peuvent être posés qu'une fois toutes les étapes créées
        for (WorkflowEtapeRequestDto e : dto.getEtapes()) {
            WorkflowEtape etape = parCode.get(e.getCodeEtape());
            etape.setEtapeSuivanteSucces(resoudre(e.getEtapeSuivanteSuccesCode(), e.getEtapeSuivanteSuccesId(), parCode, e.getCodeEtape()));
            etape.setEtapeSuivanteRejet(resoudre(e.getEtapeSuivanteRejetCode(), e.getEtapeSuivanteRejetId(), parCode, e.getCodeEtape()));
        }
        return WorkflowDefinitionResponseDto.from(repository.saveAndFlush(definition));
    }

    private void verifierEtapes(List<WorkflowEtapeRequestDto> etapes) {
        Set<String> codes = new HashSet<>();
        for (WorkflowEtapeRequestDto e : etapes) {
            if (!codes.add(e.getCodeEtape())) {
                throw new WorkflowException("Le code d'étape '" + e.getCodeEtape() + "' est utilisé plusieurs fois.");
            }
            if (e.getMontantMinimum() != null && e.getMontantMinimum().signum() < 0) {
                throw new WorkflowException("Etape " + e.getCodeEtape() + " : le montant minimum ne peut pas être négatif.");
            }
            if (e.getMontantMinimum() != null && e.getMontantMaximum() != null
                    && e.getMontantMinimum().compareTo(e.getMontantMaximum()) > 0) {
                throw new WorkflowException("Etape " + e.getCodeEtape() + " : le montant minimum dépasse le montant maximum.");
            }
        }
    }

    private WorkflowEtape resoudre(String code, UUID id, Map<String, WorkflowEtape> parCode, String etapeSource) {
        if (code != null && !code.isBlank()) {
            WorkflowEtape cible = parCode.get(code);
            if (cible == null) {
                throw new WorkflowException("Etape " + etapeSource + " : l'étape suivante '" + code + "' n'existe pas dans cette définition.");
            }
            return cible;
        }
        if (id != null) {
            // Un identifiant ne peut désigner qu'une étape déjà enregistrée, donc d'une autre définition : refusé
            if (etapeRepository.existsById(id)) {
                throw new WorkflowException("Etape " + etapeSource + " : une étape ne peut renvoyer qu'à une étape de sa propre définition ; "
                        + "utilisez etapeSuivanteSuccesCode / etapeSuivanteRejetCode.");
            }
            throw new WorkflowException("Etape " + etapeSource + " : étape suivante introuvable (" + id
                    + "). Pour relier des étapes créées dans la même requête, utilisez etapeSuivanteSuccesCode / etapeSuivanteRejetCode.");
        }
        return null;
    }
}
