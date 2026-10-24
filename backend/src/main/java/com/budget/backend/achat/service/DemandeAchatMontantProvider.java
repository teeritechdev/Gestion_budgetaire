package com.budget.backend.achat.service;

import com.budget.backend.achat.entity.DemandeAchat;
import com.budget.backend.achat.repository.DemandeAchatRepository;
import com.budget.backend.workflow.api.DossierMontantProvider;
import com.budget.backend.workflow.api.WorkflowEngineApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Indique au moteur de workflow le montant d'une demande d'achat (pour les décisions prises via son API REST). */
@Component
public class DemandeAchatMontantProvider implements DossierMontantProvider {

    @Autowired
    private DemandeAchatRepository repository;

    @Override
    public boolean supporte(String workflowCode) {
        return WorkflowEngineApi.WORKFLOW_DA.equals(workflowCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> montant(UUID dossierId) {
        return repository.findById(dossierId).map(DemandeAchat::getMontantEstimeHt);
    }
}
