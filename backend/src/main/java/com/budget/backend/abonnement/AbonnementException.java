package com.budget.backend.abonnement;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AbonnementException extends RuntimeException {
    public AbonnementException(String message) {
        super(message);
    }
}

