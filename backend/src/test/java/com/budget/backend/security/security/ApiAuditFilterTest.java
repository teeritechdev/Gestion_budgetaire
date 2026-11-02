package com.budget.backend.security.security;

import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApiAuditFilterTest {
    private final AuditService auditService = mock(AuditService.class);
    private final UtilisateurRepository utilisateurRepository = mock(UtilisateurRepository.class);
    private final ApiAuditFilter filter =
            new ApiAuditFilter(auditService, utilisateurRepository, new ObjectMapper());

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void auditsSuccessfulWritesForAnyApiResourceAndMasksSecrets() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/budget-allocations");
        request.setContentType("application/json");
        request.setContent("""
                {"montant":1250,"motDePasseHash":"must-not-be-recorded"}
                """.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (ServletRequest wrappedRequest, ServletResponse wrappedResponse) -> {
            wrappedRequest.getInputStream().readAllBytes();
            var httpResponse = (jakarta.servlet.http.HttpServletResponse) wrappedResponse;
            httpResponse.setStatus(201);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"id\":\"a9b5b67e-2135-4fde-a53c-f709b13897e7\"}");
        };

        filter.doFilter(request, response, chain);

        verify(auditService).logAction(
                isNull(),
                eq("INSERTION"),
                eq("budget-allocations"),
                eq("a9b5b67e-2135-4fde-a53c-f709b13897e7"),
                isNull(),
                argThat(values -> {
                    Object payload = values.get("donnees");
                    return payload instanceof Map<?, ?> data
                            && Integer.valueOf(1250).equals(data.get("montant"))
                            && "[MASQUÉ]".equals(data.get("motDePasseHash"));
                }),
                any(jakarta.servlet.http.HttpServletRequest.class));
        verifyNoMoreInteractions(auditService);
    }

    @Test
    void doesNotAuditFailedWritesOrAuditLogEndpoints() throws Exception {
        MockHttpServletRequest failedRequest = new MockHttpServletRequest("POST", "/api/roles");
        MockHttpServletResponse failedResponse = new MockHttpServletResponse();
        filter.doFilter(failedRequest, failedResponse, (request, response) ->
                ((jakarta.servlet.http.HttpServletResponse) response)
                        .setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST));

        MockHttpServletRequest auditRequest = new MockHttpServletRequest("POST", "/api/audit-logs");
        MockHttpServletResponse auditResponse = new MockHttpServletResponse();
        filter.doFilter(auditRequest, auditResponse, (request, response) ->
                ((jakarta.servlet.http.HttpServletResponse) response)
                        .setStatus(jakarta.servlet.http.HttpServletResponse.SC_CREATED));

        verifyNoInteractions(auditService);
    }
}
