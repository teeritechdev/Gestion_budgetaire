package com.budget.backend.abonnement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Fin de contrat : reconduction tacite ou expiration, chaque nuit (cron modifiable). */
@Slf4j
@Component
public class AbonnementScheduler {

    @Autowired
    private AbonnementService abonnementService;

    @Scheduled(cron = "${abonnement.fin-contrat.cron:0 0 2 * * *}")
    public void traiterFinsDeContrat() {
        int traites = abonnementService.traiterFinsDeContrat(LocalDate.now());
        if (traites > 0) {
            log.info("[ABONNEMENT] {} contrat(s) arrivé(s) à échéance traité(s) (reconduction ou expiration).", traites);
        }
    }
}
