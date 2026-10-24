package com.budget.backend.abonnement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Limité aux contrôleurs du Module 6 : renvoie un 400 lisible au lieu d'un 403 sans corps. */
@RestControllerAdvice(basePackages = "com.budget.backend.abonnement")
public class AbonnementExceptionHandler {

    @ExceptionHandler(AbonnementException.class)
    public ResponseEntity<Map<String, Object>> handleMetier(AbonnementException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    /** sort=... qui ne correspond à aucun champ : 400 lisible au lieu d'une erreur serveur. */
    @ExceptionHandler(org.springframework.dao.InvalidDataAccessApiUsageException.class)
    public ResponseEntity<Map<String, Object>> handleTri(org.springframework.dao.InvalidDataAccessApiUsageException e) {
        if (!com.budget.backend.common.PageUtils.estTriInvalide(e)) {
            throw e;
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", com.budget.backend.common.PageUtils.messageTriInvalide(e)));
    }

    @ExceptionHandler(org.springframework.data.mapping.PropertyReferenceException.class)
    public ResponseEntity<Map<String, Object>> handleTriDirect(org.springframework.data.mapping.PropertyReferenceException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", com.budget.backend.common.PageUtils.messageTriInvalide(e)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> erreurs.put(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Données invalides", "erreurs", erreurs));
    }
}
