package com.budget.backend.security.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    @Test
    void bloqueApresTropDEchecsPuisDebloqueApresUnSucces() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < LoginAttemptService.MAX_ECHECS - 1; i++) {
            service.echec("Agent@UAB.bf");
        }
        assertFalse(service.estBloque("agent@uab.bf"));

        service.echec("agent@uab.bf");
        assertTrue(service.estBloque("agent@uab.bf"));

        service.succes("agent@uab.bf");
        assertFalse(service.estBloque("agent@uab.bf"));
    }
}
