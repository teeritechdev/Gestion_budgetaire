package com.budget.backend.security.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdresseIpResolverTest {

    private static MockHttpServletRequest requete(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    @Test
    void conserveLAdresseDUnPosteDistant() {
        AdresseIpResolver resolver = new AdresseIpResolver(List.of());

        assertEquals("192.168.1.42", resolver.adresseClient(requete("192.168.1.42")));
    }

    @Test
    void ignoreXForwardedForSansProxyDeConfiance() {
        AdresseIpResolver resolver = new AdresseIpResolver(List.of());
        MockHttpServletRequest request = requete("192.168.1.42");
        request.addHeader("X-Forwarded-For", "8.8.8.8");

        assertEquals("192.168.1.42", resolver.adresseClient(request));
    }

    @Test
    void litXForwardedForDerriereUnProxyDeConfiance() {
        AdresseIpResolver resolver = new AdresseIpResolver(List.of("10.0.0.5"));
        MockHttpServletRequest request = requete("10.0.0.5");
        request.addHeader("X-Forwarded-For", " 192.168.1.42 , 10.0.0.5");

        assertEquals("192.168.1.42", resolver.adresseClient(request));
    }

    @Test
    void ignoreUnXForwardedForInvalide() {
        AdresseIpResolver resolver = new AdresseIpResolver(List.of("10.0.0.5"));
        MockHttpServletRequest request = requete("10.0.0.5");
        request.addHeader("X-Forwarded-For", "<script>, n'importe quoi");

        assertEquals("10.0.0.5", resolver.adresseClient(request));
    }

    @Test
    void remplaceLeBouclageParLAdresseDeLaMachine() {
        AdresseIpResolver resolver = new AdresseIpResolver(List.of());
        String machine = resolver.adresseMachine();

        String adresse = resolver.adresseClient(requete("0:0:0:0:0:0:0:1"));

        assertEquals(machine == null ? "::1" : machine, adresse);
        if (machine != null) assertFalse(AdresseIpResolver.estBouclage(adresse));
    }

    @Test
    void normaliseLesFormats() {
        assertEquals("192.168.1.42", AdresseIpResolver.normaliser("::ffff:192.168.1.42"));
        assertEquals("::1", AdresseIpResolver.normaliser("0:0:0:0:0:0:0:1"));
        assertEquals("fe80::1", AdresseIpResolver.normaliser("[FE80::1%eth0]"));
        assertNull(AdresseIpResolver.normaliser("999.1.1.1"));
        assertNull(AdresseIpResolver.normaliser("unknown"));
        assertTrue(AdresseIpResolver.estBouclage("127.0.0.1"));
    }
}
