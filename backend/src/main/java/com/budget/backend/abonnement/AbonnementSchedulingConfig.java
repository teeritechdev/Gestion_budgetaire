package com.budget.backend.abonnement;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Active les tâches planifiées (fin de contrat) sans toucher à la classe principale de l'application. */
@Configuration
@EnableScheduling
public class AbonnementSchedulingConfig {
}
