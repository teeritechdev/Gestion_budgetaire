package com.budget.backend.security.dto;


import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    @JsonAlias("email")
    private String username;

    @NotBlank
    private String password;
}
