package com.budget.backend.stock;

import com.budget.backend.stock.service.NumeroDocumentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NumeroDocumentServiceTest {

    private NumeroDocumentService service;
    private EntityManager em;

    @BeforeEach
    void setUp() {
        service = new NumeroDocumentService();
        em = mock(EntityManager.class);
        ReflectionTestUtils.setField(service, "em", em);
        Query q = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(q);
        when(q.getSingleResult()).thenReturn(42L);
    }

    @Test
    void formatPrefixeAnneeNumeroSurQuatreChiffres() {
        String an = String.valueOf(Year.now().getValue());
        assertEquals("BL-" + an + "-0042", service.prochainNumero("BL", "seq_bon_livraison"));
        assertEquals("PV-REC-" + an + "-0042", service.prochainNumero("PV-REC", "seq_pv_reception"));
        assertEquals("DA-" + an + "-0042", service.prochainCodeDemandeAchat());
        verify(em).createNativeQuery("select nextval('seq_bon_livraison')");
    }

    @Test
    void prefixeOuSequenceSuspectsRefuses() {
        assertThrows(IllegalArgumentException.class, () -> service.prochainNumero("bl", "seq_x"));
        assertThrows(IllegalArgumentException.class, () -> service.prochainNumero("BL", "articles"));
        assertThrows(IllegalArgumentException.class, () -> service.prochainNumero("BL", "seq_x'); drop table articles; --"));
        assertThrows(IllegalArgumentException.class, () -> service.prochainNumero(null, "seq_x"));
        assertThrows(IllegalArgumentException.class, () -> service.prochainNumero("BL", null));
        verify(em, never()).createNativeQuery(anyString());
    }
}
