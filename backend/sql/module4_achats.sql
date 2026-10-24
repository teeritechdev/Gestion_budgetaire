-- Module 4 : Achats, Expression des besoins & P2P (conforme au MRD, BDD_Budget.pdf)
-- Prérequis : Module 1 (utilisateurs, structures, entites), Module 2 (budget_allocation_structure), Module 3 (articles).
-- NB : activite_id référence budget_allocation_structure (équivalent, côté code, de "activites_budgetaires" du MRD).

CREATE TABLE IF NOT EXISTS fournisseurs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_fournisseur VARCHAR(50) UNIQUE NOT NULL,
    raison_sociale VARCHAR(200) NOT NULL,
    ifu VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(150),
    telephone VARCHAR(50),
    adresse TEXT,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS demandes_achat (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_dossier VARCHAR(50) UNIQUE NOT NULL,
    entite_emettrice VARCHAR(20) NOT NULL REFERENCES entites(code),
    entite_beneficiaire VARCHAR(20) NOT NULL REFERENCES entites(code),
    type_achat VARCHAR(30) NOT NULL,
    objet VARCHAR(255) NOT NULL,
    justification TEXT,
    montant_estime_ht NUMERIC(15,2) DEFAULT 0.00,
    montant_estime_ttc NUMERIC(15,2) DEFAULT 0.00,
    statut VARCHAR(30) DEFAULT 'SOUMIS',
    etape_courante VARCHAR(50),
    demandeur_id UUID NOT NULL REFERENCES utilisateurs(id),
    direction_demandeur_id UUID NOT NULL REFERENCES structures(id),
    exercice_annee INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lignes_demande_achat (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    da_id UUID NOT NULL REFERENCES demandes_achat(id) ON DELETE CASCADE,
    activite_id UUID NOT NULL REFERENCES budget_allocation_structure(id),
    article_id UUID REFERENCES articles(id),
    description TEXT NOT NULL,
    unite_mesure VARCHAR(30),
    quantite NUMERIC(15,3) NOT NULL,
    prix_unitaire_estime NUMERIC(15,2) NOT NULL,
    montant_total_ht NUMERIC(15,2) GENERATED ALWAYS AS (ROUND(quantite * prix_unitaire_estime, 2)) STORED,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dossier_ventilation_impacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    da_id UUID NOT NULL REFERENCES demandes_achat(id) ON DELETE CASCADE,
    entite_impactee VARCHAR(20) NOT NULL REFERENCES entites(code),
    activite_impactee_id UUID REFERENCES budget_allocation_structure(id),
    montant_impute NUMERIC(15,2) NOT NULL,
    pourcentage_applique NUMERIC(5,2) NOT NULL,
    statut_blocage BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS historique_validations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    da_id UUID NOT NULL REFERENCES demandes_achat(id) ON DELETE CASCADE,
    etape VARCHAR(50) NOT NULL,
    valideur_id UUID NOT NULL REFERENCES utilisateurs(id),
    decision VARCHAR(30) NOT NULL,
    commentaire TEXT,
    date_decision TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bons_commande (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_bc VARCHAR(50) UNIQUE NOT NULL,
    type_bc VARCHAR(30) DEFAULT 'CLASSIQUE',
    da_id UUID NOT NULL REFERENCES demandes_achat(id),
    fournisseur_id UUID NOT NULL REFERENCES fournisseurs(id),
    montant_total_ht NUMERIC(15,2) NOT NULL,
    montant_total_ttc NUMERIC(15,2) NOT NULL,
    montant_facture_ttc NUMERIC(15,2) DEFAULT 0.00,
    solde_restant_ttc NUMERIC(15,2) GENERATED ALWAYS AS (montant_total_ttc - montant_facture_ttc) STORED,
    statut VARCHAR(30) DEFAULT 'EMIS',
    date_emission DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lignes_bon_commande (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bc_id UUID NOT NULL REFERENCES bons_commande(id) ON DELETE CASCADE,
    ligne_da_id UUID NOT NULL REFERENCES lignes_demande_achat(id),
    article_id UUID REFERENCES articles(id),
    description TEXT NOT NULL,
    quantite_commandee NUMERIC(15,3) NULL,
    prix_unitaire_ht NUMERIC(15,2) NULL,
    montant_total_ht NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Séquences de numérotation (DA-AAAA-NNNN, BC-AAAA-NNNN, FOURN-AAAA-NNNN)
CREATE SEQUENCE IF NOT EXISTS seq_demande_achat START 1;
CREATE SEQUENCE IF NOT EXISTS seq_bon_commande START 1;
CREATE SEQUENCE IF NOT EXISTS seq_fournisseur START 1;
