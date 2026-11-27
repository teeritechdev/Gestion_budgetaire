-- ==============================================================================
-- MODULE 4 : ACHATS, EXPRESSION DES BESOINS & P2P (UAB ASSURANCES)
-- ==============================================================================

-- 1. Séquences de numérotation automatique
CREATE SEQUENCE IF NOT EXISTS seq_fournisseur START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_demande_achat START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_bon_commande START WITH 1 INCREMENT BY 1;

-- 2. Table Référentiel des Fournisseurs
CREATE TABLE IF NOT EXISTS fournisseurs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_fournisseur VARCHAR(50) UNIQUE NOT NULL,
    raison_sociale VARCHAR(255) NOT NULL,
    ifu VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(150),
    telephone VARCHAR(50),
    adresse TEXT,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Table Demandes d'Achat (DA)
CREATE TABLE IF NOT EXISTS demandes_achat (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_dossier VARCHAR(50) UNIQUE NOT NULL,
    entite_emettrice VARCHAR(20) NOT NULL,
    entite_beneficiaire VARCHAR(20) NOT NULL,
    type_achat VARCHAR(30) NOT NULL,
    objet VARCHAR(255) NOT NULL,
    justification TEXT,
    montant_estime_ht NUMERIC(15,2) DEFAULT 0.00,
    montant_estime_ttc NUMERIC(15,2) DEFAULT 0.00,
    statut VARCHAR(30) DEFAULT 'SOUMIS',
    etape_courante VARCHAR(50) DEFAULT 'VALIDATION_N1',
    demandeur_id UUID NOT NULL REFERENCES utilisateurs(id),
    service_demandeur_id UUID REFERENCES services(id),
    exercice_annee INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Table Lignes de Demande d'Achat
CREATE TABLE IF NOT EXISTS lignes_demande_achat (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    da_id UUID NOT NULL REFERENCES demandes_achat(id) ON DELETE CASCADE,
    activite_id UUID NOT NULL REFERENCES activites_catalogue(id),
    article_id UUID REFERENCES articles(id),
    description TEXT NOT NULL,
    unite_mesure VARCHAR(30) DEFAULT 'Unité',
    quantite INT NOT NULL DEFAULT 1,
    prix_unitaire_estime NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    montant_total_ht NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Table Historique des Validations
CREATE TABLE IF NOT EXISTS historique_validations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    da_id UUID NOT NULL REFERENCES demandes_achat(id) ON DELETE CASCADE,
    etape VARCHAR(50) NOT NULL,
    valideur_id UUID NOT NULL REFERENCES utilisateurs(id),
    decision VARCHAR(30) NOT NULL,
    commentaire TEXT,
    date_decision TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Table Bons de Commande (BC)
CREATE TABLE IF NOT EXISTS bons_commande (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_bc VARCHAR(50) UNIQUE NOT NULL,
    type_bc VARCHAR(30) DEFAULT 'CLASSIQUE',
    da_id UUID NOT NULL REFERENCES demandes_achat(id),
    fournisseur_id UUID NOT NULL REFERENCES fournisseurs(id),
    montant_total_ht NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    montant_total_ttc NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    montant_facture_ttc NUMERIC(15,2) DEFAULT 0.00,
    statut VARCHAR(30) DEFAULT 'EMIS',
    date_emission DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Table Lignes de Bon de Commande
CREATE TABLE IF NOT EXISTS lignes_bon_commande (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bc_id UUID NOT NULL REFERENCES bons_commande(id) ON DELETE CASCADE,
    ligne_da_id UUID REFERENCES lignes_demande_achat(id),
    article_id UUID REFERENCES articles(id),
    description TEXT NOT NULL,
    quantite_commandee INT,
    prix_unitaire_ht NUMERIC(15,2),
    montant_total_ht NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Table Ventilation des Impacts Analytiques & Inter-Filiales
CREATE TABLE IF NOT EXISTS dossier_ventilation_impacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    da_id UUID NOT NULL REFERENCES demandes_achat(id) ON DELETE CASCADE,
    entite_impactee VARCHAR(20) NOT NULL,
    activite_impactee_id UUID REFERENCES activites_catalogue(id),
    montant_impute NUMERIC(15,2) NOT NULL,
    pourcentage_applique NUMERIC(5,2) NOT NULL,
    statut_blocage BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index de performance
CREATE INDEX IF NOT EXISTS idx_da_demandeur ON demandes_achat(demandeur_id);
CREATE INDEX IF NOT EXISTS idx_da_statut ON demandes_achat(statut);
CREATE INDEX IF NOT EXISTS idx_lignes_da_da ON lignes_demande_achat(da_id);
CREATE INDEX IF NOT EXISTS idx_hist_val_da ON historique_validations(da_id);
CREATE INDEX IF NOT EXISTS idx_bc_fournisseur ON bons_commande(fournisseur_id);
CREATE INDEX IF NOT EXISTS idx_bc_da ON bons_commande(da_id);
CREATE INDEX IF NOT EXISTS idx_lignes_bc_bc ON lignes_bon_commande(bc_id);
CREATE INDEX IF NOT EXISTS idx_ventilation_da ON dossier_ventilation_impacts(da_id);

