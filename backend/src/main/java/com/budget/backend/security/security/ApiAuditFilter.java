package com.budget.backend.security.security;

import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.service.AuditService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

final class ApiAuditFilter extends OncePerRequestFilter {
    private static final String MANUAL_AUDIT_ATTRIBUTE = AuditService.class.getName() + ".logged";
    private static final int MAX_AUDITED_BODY_SIZE = 32 * 1024;
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final AuditService auditService;
    private final UtilisateurRepository utilisateurRepository;
    private final ObjectMapper objectMapper;

    ApiAuditFilter(
            AuditService auditService,
            UtilisateurRepository utilisateurRepository,
            ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.utilisateurRepository = utilisateurRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        return !path.startsWith("/api/")
                || path.startsWith("/api/audit-logs")
                || !(method.equals("POST") || method.equals("PUT")
                || method.equals("PATCH") || method.equals("DELETE"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, MAX_AUDITED_BODY_SIZE);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        UUID utilisateurId = authenticatedUserId();
        boolean completed = false;

        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
            completed = true;
        } finally {
            try {
                if (completed
                        && cachedResponse.getStatus() < HttpServletResponse.SC_BAD_REQUEST
                        && !Boolean.TRUE.equals(cachedRequest.getAttribute(MANUAL_AUDIT_ATTRIBUTE))) {
                    enregistrerAction(cachedRequest, cachedResponse, utilisateurId);
                }
            } finally {
                cachedResponse.copyBodyToResponse();
            }
        }
    }

    private void enregistrerAction(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            UUID utilisateurId) throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String[] segments = path.split("/");
        String nomTable = segments.length > 2 ? segments[2] : "api";
        String typeAction = typeAction(request.getMethod(), segments);
        Map<String, Object> nouvellesValeurs = nouvellesValeurs(request, response, path);
        String enregistrementId = enregistrementId(segments, response);

        auditService.logAction(
                utilisateurId,
                typeAction,
                nomTable.substring(0, Math.min(nomTable.length(), 50)),
                enregistrementId,
                null,
                nouvellesValeurs,
                request);
    }

    private UUID authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return null;
        }
        return utilisateurRepository.findByUsername(userDetails.getUsername())
                .map(Utilisateur::getId)
                .orElse(null);
    }

    private Map<String, Object> nouvellesValeurs(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            String path) throws IOException {
        Map<String, Object> values = new HashMap<>();
        values.put("endpoint", path);
        values.put("methodeHttp", request.getMethod());
        values.put("statutHttp", response.getStatus());

        byte[] body = request.getContentAsByteArray();
        if (request.getContentLengthLong() > MAX_AUDITED_BODY_SIZE
                || body.length >= MAX_AUDITED_BODY_SIZE) {
            values.put("donnees", "Corps de requête omis car supérieur à 32 Ko.");
        } else if (body.length > 0 && request.getContentType() != null
                && request.getContentType().contains("application/json")) {
            JsonNode payload = objectMapper.readTree(body);
            if (payload != null && payload.isObject()) {
                values.put("donnees", masquerChampsSensibles(
                        objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() {})));
            } else if (payload != null && payload.isArray()) {
                values.put("donnees", masquerChampsSensibles(
                        objectMapper.convertValue(payload, new TypeReference<List<Object>>() {})));
            }
        }
        return values;
    }

    private Map<String, Object> masquerChampsSensibles(Map<String, Object> values) {
        Map<String, Object> sanitized = new HashMap<>();
        values.forEach((key, value) -> sanitized.put(key, masquerValeur(key, value)));
        return sanitized;
    }

    private List<Object> masquerChampsSensibles(List<Object> values) {
        return values.stream().map(value -> masquerValeur("", value)).toList();
    }

    private Object masquerValeur(String key, Object value) {
        String normalizedKey = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (normalizedKey.contains("password")
                || normalizedKey.contains("motdepasse")
                || normalizedKey.contains("token")
                || normalizedKey.contains("secret")
                || normalizedKey.contains("hash")) {
            return "[MASQUÉ]";
        }
        if (value instanceof Map<?, ?> nested) {
            return masquerChampsSensibles(toStringKeyMap(nested));
        }
        if (value instanceof List<?> list) {
            return list.stream().map(item -> masquerValeur("", item)).toList();
        }
        return value;
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> values) {
        Map<String, Object> mapped = new HashMap<>();
        values.forEach((key, value) -> mapped.put(String.valueOf(key), value));
        return mapped;
    }

    private String enregistrementId(String[] segments, ContentCachingResponseWrapper response) throws IOException {
        for (int i = segments.length - 1; i >= 0; i--) {
            if (UUID_PATTERN.matcher(segments[i]).matches()) {
                return segments[i];
            }
        }

        byte[] body = response.getContentAsByteArray();
        if (body.length > 0 && response.getContentType() != null
                && response.getContentType().contains("application/json")) {
            JsonNode result = objectMapper.readTree(body);
            if (result != null && result.isObject() && result.hasNonNull("id")) {
                return result.get("id").asText();
            }
        }
        return "collection";
    }

    private String typeAction(String method, String[] segments) {
        if (method.equals("DELETE")) {
            return "SUPPRESSION";
        }
        if (method.equals("PUT") || method.equals("PATCH")) {
            return "MODIFICATION";
        }
        for (String segment : segments) {
            if (UUID_PATTERN.matcher(segment).matches()) {
                return "MODIFICATION";
            }
        }
        return "INSERTION";
    }
}
