package com.budget.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduit les exceptions applicatives en réponses JSON homogènes { status, message, errors? }
 * afin que le frontend puisse toujours afficher un message lisible.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> gererStatut(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();
        return corps(status, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> gererValidation(MethodArgumentNotValidException exception) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                erreurs.putIfAbsent(error.getField(), error.getDefaultMessage()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Les données du formulaire sont invalides.");
        body.put("errors", erreurs);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> gererCorpsIllisible(HttpMessageNotReadableException exception) {
        return corps(HttpStatus.BAD_REQUEST, "Le corps de la requête est invalide ou mal formé.");
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<Map<String, Object>> gererIdentifiants(BadCredentialsException exception) {
        return corps(HttpStatus.UNAUTHORIZED, "Identifiant ou mot de passe incorrect.");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> gererCompteDesactive(DisabledException exception) {
        return corps(HttpStatus.UNAUTHORIZED, "Ce compte est désactivé.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> gererAuthentification(AuthenticationException exception) {
        return corps(HttpStatus.UNAUTHORIZED, "Authentification requise.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> gererAccesRefuse(AccessDeniedException exception) {
        return corps(HttpStatus.FORBIDDEN, "Vous n'avez pas les permissions requises pour cette action.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> gererIntegrite(DataIntegrityViolationException exception) {
        log.warn("Violation d'intégrité des données : {}", exception.getMostSpecificCause().getMessage());
        return corps(HttpStatus.CONFLICT,
                "L'opération viole une contrainte d'intégrité (doublon ou enregistrement encore référencé).");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> gererArgument(IllegalArgumentException exception) {
        return corps(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * Les services historiques lèvent des RuntimeException avec un message métier
     * (« introuvable », « dépasse l'enveloppe »...). On les renvoie en 400/404
     * avec leur message au lieu d'une erreur 500 opaque.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> gererMetier(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()
                || exception.getClass() != RuntimeException.class
                || exception.getCause() != null) {
            log.error("Erreur interne non gérée", exception);
            return corps(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue.");
        }
        HttpStatus status = message.toLowerCase().contains("introuvable") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return corps(status, message);
    }

    private ResponseEntity<Map<String, Object>> corps(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
