package com.budget.backend.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvUtilsTest {

    @Test
    void protegeLesSeparateursGuillemetsEtRetoursALaLigne() {
        assertEquals("abc;\"a;b\";\"dit \"\"oui\"\"\";\"l1\nl2\";\n",
                CsvUtils.ligne(';', "abc", "a;b", "dit \"oui\"", "l1\nl2", null));
    }

    @Test
    void neutraliseLesFormulesSansToucherAuxTelephonesNiAuxNombres() {
        assertEquals("'=1+1", CsvUtils.cellule("=1+1", ','));
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", CsvUtils.cellule("=HYPERLINK(\"x\")", ','));
        assertEquals("'@SUM(A1)", CsvUtils.cellule("@SUM(A1)", ','));
        assertEquals("+226 70 00 00 00", CsvUtils.cellule("+226 70 00 00 00", ';'));
        assertEquals("-40", CsvUtils.cellule(new BigDecimal("-40"), ','));
    }
}
