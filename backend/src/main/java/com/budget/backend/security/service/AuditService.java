package com.budget.backend.security.service;


import com.budget.backend.security.entity.AuditLog;
import com.budget.backend.security.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private HttpServletRequest request;

    public void logAction(UUID utilisateurId, String typeAction, String nomTable, String enregistrementId,
                          Map<String, Object> anciennesValeurs, Map<String, Object> nouvellesValeurs) {

        String adresseIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        AuditLog auditLog = AuditLog.builder()
                .utilisateurId(utilisateurId)
                .typeAction(typeAction)
                .nomTable(nomTable)
                .enregistrementId(enregistrementId)
                .anciennesValeurs(anciennesValeurs)
                .nouvellesValeurs(nouvellesValeurs)
                .adresseIp(adresseIp)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(auditLog);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
