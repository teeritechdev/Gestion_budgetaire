package com.budget.backend.abonnement.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcul pur (sans base de données) des dates d'échéance d'un contrat.
 * Règle du MRD : jour_facturation = 31 signifie « dernier jour du mois » ; un jour absent d'un mois court
 * s'ajuste au dernier jour de ce mois (28/29 février, 30 avril...).
 */
public final class CalendrierEcheances {

    private CalendrierEcheances() {
    }

    /** Nombre de mois entre deux échéances (MENSUEL=1, TRIMESTRIEL=3, SEMESTRIEL=6, ANNUEL=12). */
    public static int pasEnMois(String periodicite) {
        return switch (periodicite) {
            case "MENSUEL" -> 1;
            case "TRIMESTRIEL" -> 3;
            case "SEMESTRIEL" -> 6;
            case "ANNUEL" -> 12;
            default -> throw new IllegalArgumentException("Périodicité inconnue : " + periodicite);
        };
    }

    /**
     * Dates d'échéance comprises entre dateDebut et dateFin (bornes incluses), en partant du mois de dateDebut.
     * Une première date antérieure à dateDebut (ex. jour 5 pour un contrat commençant le 15) est ignorée.
     */
    public static List<LocalDate> generer(LocalDate dateDebut, LocalDate dateFin, String periodicite, int jourFacturation) {
        int pas = pasEnMois(periodicite);
        List<LocalDate> dates = new ArrayList<>();
        YearMonth ancre = YearMonth.from(dateDebut);
        for (int k = 0; ; k++) {
            YearMonth mois = ancre.plusMonths((long) k * pas);
            LocalDate date = mois.atDay(Math.min(jourFacturation, mois.lengthOfMonth()));
            if (date.isAfter(dateFin)) {
                break;
            }
            if (!date.isBefore(dateDebut)) {
                dates.add(date);
            }
        }
        return dates;
    }
}
