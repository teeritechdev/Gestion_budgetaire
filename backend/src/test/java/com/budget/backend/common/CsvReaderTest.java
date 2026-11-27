package com.budget.backend.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvReaderTest {

    private static MockMultipartFile fichier(String contenu) {
        return new MockMultipartFile("file", "import.csv", "text/csv", contenu.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void litLesColonnesParNomQuelQueSoitLOrdreLesAccentsEtLeSeparateur() {
        String csv = "﻿Libellé;Code Plan;Date Début;Date Fin;Actif\n"
                + "\"Plan; ambitieux\";PS-2026;01/01/2026;2030-12-31;non\n\n";
        List<String> codes = new ArrayList<>();
        int lues = CsvReader.lire(fichier(csv), List.of("code_plan", "libelle"), ligne -> {
            codes.add(ligne.texte("code_plan"));
            assertEquals("Plan; ambitieux", ligne.texte("libelle"));
            assertEquals(LocalDate.of(2026, 1, 1), ligne.date("date_debut"));
            assertEquals(LocalDate.of(2030, 12, 31), ligne.date("date_fin"));
            assertFalse(ligne.booleen(true, "actif"));
        });
        assertEquals(1, lues);
        assertEquals(List.of("PS-2026"), codes);
    }

    @Test
    void relitUnFichierProduitParLExport() {
        String export = CsvUtils.ligne(',', "ID", "Code OS", "Libellé", "Description")
                + CsvUtils.ligne(',', UUID.randomUUID(), "OS-01", "Croissance", "Ligne 1\nLigne \"2\"");
        CsvReader.lire(fichier(export), List.of("code_os", "libelle"), ligne -> {
            assertEquals("OS-01", ligne.texte("code_os"));
            assertEquals("Ligne 1\nLigne \"2\"", ligne.texte("description"));
        });
    }

    @Test
    void comprendLesMontantsAuFormatFrancais() {
        CsvReader.lire(fichier("montant\n1 250,50\n"), List.of("montant"),
                ligne -> assertEquals(new BigDecimal("1250.50"), ligne.decimal("montant")));
    }

    @Test
    void signaleLesColonnesManquantesEtLeNumeroDeLigneEnErreur() {
        ResponseStatusException manquante = assertThrows(ResponseStatusException.class,
                () -> CsvReader.lire(fichier("code\nA\n"), List.of("code", "libelle"), ligne -> { }));
        assertTrue(manquante.getReason().contains("libelle"));

        ResponseStatusException ligne3 = assertThrows(ResponseStatusException.class,
                () -> CsvReader.lire(fichier("montant\n10\nabc\n"), List.of("montant"), ligne -> ligne.decimal("montant")));
        assertTrue(ligne3.getReason().startsWith("Ligne 3"));
    }

    @Test
    void lExportRespecteLOrdreDAffichageEtIgnoreLesIdentifiantsInconnus() {
        List<String> ordre = List.of("c", "a", "inconnu", "b");
        List<String> resultat = TransfertDonneesController.ordonner(ordre, List.of("a", "b", "c"), s -> s);
        assertEquals(List.of("c", "a", "b"), resultat);
    }
}
