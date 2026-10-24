package com.budget.backend.abonnement;

import com.budget.backend.abonnement.service.CalendrierEcheances;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalendrierEcheancesTest {

    private static LocalDate d(int y, int m, int j) {
        return LocalDate.of(y, m, j);
    }

    @Test
    void scenarioGardiennageDouzeEcheancesFinDeMois() {
        // Exemple du PDF : 01/01/2026 -> 31/12/2026, mensuel, jour 31 (= dernier jour du mois)
        List<LocalDate> dates = CalendrierEcheances.generer(d(2026, 1, 1), d(2026, 12, 31), "MENSUEL", 31);

        assertEquals(12, dates.size());
        assertEquals(d(2026, 1, 31), dates.get(0));
        assertEquals(d(2026, 2, 28), dates.get(1));   // février s'arrête au 28
        assertEquals(d(2026, 3, 31), dates.get(2));
        assertEquals(d(2026, 4, 30), dates.get(3));   // avril s'arrête au 30
        assertEquals(d(2026, 12, 31), dates.get(11));
    }

    @Test
    void anneeBissextileFevrierAuVingtNeuf() {
        List<LocalDate> dates = CalendrierEcheances.generer(d(2028, 1, 1), d(2028, 3, 31), "MENSUEL", 31);
        assertEquals(List.of(d(2028, 1, 31), d(2028, 2, 29), d(2028, 3, 31)), dates);
    }

    @Test
    void jourFixeDansLeMois() {
        List<LocalDate> dates = CalendrierEcheances.generer(d(2026, 1, 1), d(2026, 3, 31), "MENSUEL", 5);
        assertEquals(List.of(d(2026, 1, 5), d(2026, 2, 5), d(2026, 3, 5)), dates);
    }

    @Test
    void trimestrielSemestrielEtAnnuel() {
        assertEquals(List.of(d(2026, 1, 15), d(2026, 4, 15), d(2026, 7, 15), d(2026, 10, 15)),
                CalendrierEcheances.generer(d(2026, 1, 1), d(2026, 12, 31), "TRIMESTRIEL", 15));
        assertEquals(List.of(d(2026, 1, 1), d(2026, 7, 1)),
                CalendrierEcheances.generer(d(2026, 1, 1), d(2026, 12, 31), "SEMESTRIEL", 1));
        assertEquals(List.of(d(2026, 6, 30), d(2027, 6, 30), d(2028, 6, 30)),
                CalendrierEcheances.generer(d(2026, 6, 1), d(2028, 12, 31), "ANNUEL", 31));
    }

    @Test
    void premiereDateAvantLeDebutDuContratEstIgnoree() {
        // contrat commençant le 15 janvier, jour de facturation 5 : la première échéance est le 5 février
        List<LocalDate> dates = CalendrierEcheances.generer(d(2026, 1, 15), d(2026, 3, 31), "MENSUEL", 5);
        assertEquals(List.of(d(2026, 2, 5), d(2026, 3, 5)), dates);
    }

    @Test
    void lesBornesSontIncluses() {
        assertEquals(List.of(d(2026, 1, 31)), CalendrierEcheances.generer(d(2026, 1, 31), d(2026, 1, 31), "MENSUEL", 31));
        // fin au 30 juin : l'échéance du 30 juin est incluse, celle du 31 juillet non
        assertEquals(6, CalendrierEcheances.generer(d(2026, 1, 1), d(2026, 6, 30), "MENSUEL", 31).size());
    }

    @Test
    void finAvantLaPremiereEcheanceDonneUneListeVide() {
        assertTrue(CalendrierEcheances.generer(d(2026, 1, 1), d(2026, 1, 3), "MENSUEL", 20).isEmpty());
    }

    @Test
    void periodiciteInconnueRefusee() {
        assertThrows(IllegalArgumentException.class, () -> CalendrierEcheances.generer(d(2026, 1, 1), d(2026, 12, 31), "QUOTIDIEN", 1));
    }
}
