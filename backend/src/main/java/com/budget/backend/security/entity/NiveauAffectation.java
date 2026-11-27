package com.budget.backend.security.entity;

/** Niveau de l'organisation auquel un salarié est affecté. */
public enum NiveauAffectation {
    STRUCTURE("Structure"),
    DIRECTION("Direction"),
    SERVICE("Service");

    private final String libelle;

    NiveauAffectation(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
