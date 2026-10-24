-- Module 6 : Achats récurrents & contrats cadres (conforme au MRD, BDD_Budget.pdf)
-- Prérequis : Module 1 (utilisateurs), Module 2 (budget_allocation_structure), Module 4 (fournisseurs, bons_commande).
-- NB : activite_id référence budget_allocation_structure (équivalent, côté code, de "activites_budgetaires" du MRD).

CREATE TABLE IF NOT EXISTS abonnements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_abonnement VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    fournisseur_id UUID NOT NULL REFERENCES fournisseurs(id),
    activite_id UUID NOT NULL REFERENCES budget_allocation_structure(id),
    periodicite VARCHAR(20) NOT NULL,          -- MENSUEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL
    jour_facturation INT NOT NULL CHECK (jour_facturation BETWEEN 1 AND 31),  -- 31 = dernier jour du mois
    montant_echeance_ht NUMERIC(15,2) NOT NULL,
    montant_echeance_ttc NUMERIC(15,2) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    tacite_reconduction BOOLEAN DEFAULT FALSE,
    preavis_jours INT,
    statut VARCHAR(20) DEFAULT 'BROUILLON',    -- BROUILLON, ACTIF, SUSPENDU, RESILIE, EXPIRE
    responsable_id UUID NOT NULL REFERENCES utilisateurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS echeances_abonnement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    abonnement_id UUID NOT NULL REFERENCES abonnements(id) ON DELETE CASCADE,
    num_echeance INT NOT NULL,
    date_echeance DATE NOT NULL,
    montant_attendu_ttc NUMERIC(15,2) NOT NULL,
    montant_facture_ttc NUMERIC(15,2),
    statut VARCHAR(20) DEFAULT 'A_VENIR',      -- A_VENIR, BC_GENERE, FACTURE_RECUE, PAYE, ANNULE
    bc_id UUID REFERENCES bons_commande(id),
    reference_facture VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (abonnement_id, num_echeance),
    UNIQUE (abonnement_id, date_echeance)
);

CREATE SEQUENCE IF NOT EXISTS seq_abonnement START 1;
