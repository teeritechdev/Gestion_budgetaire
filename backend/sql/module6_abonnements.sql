-- ==============================================================================
-- MODULE 6 : ACHATS RÉCURRENTS & CONTRATS CADRES (UAB ASSURANCES)
-- ==============================================================================

CREATE SEQUENCE IF NOT EXISTS seq_abonnement START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS abonnements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_abonnement VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    fournisseur_id UUID NOT NULL REFERENCES fournisseurs(id),
    activite_id UUID NOT NULL REFERENCES activites_catalogue(id),
    periodicite VARCHAR(30) NOT NULL DEFAULT 'MENSUEL',
    jour_facturation INT NOT NULL DEFAULT 31,
    montant_echeance_ht NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    montant_echeance_ttc NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    tacite_reconduction BOOLEAN DEFAULT FALSE,
    preavis_jours INT DEFAULT 30,
    statut VARCHAR(30) DEFAULT 'ACTIF',
    responsable_id UUID REFERENCES utilisateurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS echeances_abonnement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    abonnement_id UUID NOT NULL REFERENCES abonnements(id) ON DELETE CASCADE,
    num_echeance INT NOT NULL,
    date_echeance DATE NOT NULL,
    montant_attendu_ttc NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    montant_facture_ttc NUMERIC(15,2) DEFAULT 0.00,
    statut VARCHAR(30) DEFAULT 'A_VENIR',
    bc_id UUID REFERENCES bons_commande(id),
    reference_facture VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_abn_fournisseur ON abonnements(fournisseur_id);
CREATE INDEX IF NOT EXISTS idx_abn_activite ON abonnements(activite_id);
CREATE INDEX IF NOT EXISTS idx_abn_statut ON abonnements(statut);
CREATE INDEX IF NOT EXISTS idx_echeances_abn ON echeances_abonnement(abonnement_id);
CREATE INDEX IF NOT EXISTS idx_echeances_statut ON echeances_abonnement(statut);

