-- Module 3 : Articles & Stocks. Version alignée sur le modèle organisationnel de Djibrila :
--   * demandes_magasin.service_id    -> services(id)         (service de rattachement)
--   * demandes_magasin.structure_code -> structures(code)     (ex-« entite_code » ; « entites » est devenue « structures »)
-- Prérequis : migrations db/manual V20261029_01 à V20261030_02 appliquées (tables structures, directions, services),
--             et table utilisateurs. Base déjà créée avec l'ancien script : voir module3_stock_migration_structures.sql.

CREATE TABLE IF NOT EXISTS articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_article VARCHAR(50) UNIQUE NOT NULL,
    designation VARCHAR(255) NOT NULL,
    description TEXT,
    unite_mesure VARCHAR(30) NOT NULL,
    quantite_stock INT NOT NULL DEFAULT 0 CHECK (quantite_stock >= 0),
    seuil_reapprovisionnement INT DEFAULT 0 CHECK (seuil_reapprovisionnement IS NULL OR seuil_reapprovisionnement >= 0),
    prix_unitaire_moyen_pondere NUMERIC(15,2) DEFAULT 0.00 CHECK (prix_unitaire_moyen_pondere IS NULL OR prix_unitaire_moyen_pondere >= 0),
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS demandes_magasin (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_demande VARCHAR(50) UNIQUE NOT NULL,
    demandeur_id UUID NOT NULL REFERENCES utilisateurs(id),
    service_id UUID NOT NULL REFERENCES services(id),
    structure_code VARCHAR(20) NOT NULL REFERENCES structures(code),
    objet VARCHAR(255) NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'SOUMIS'
        CHECK (statut IN ('SOUMIS','VALIDE_N1','PARTIELLEMENT_SERVIE','SERVIE','REJETEE')),
    valideur_n1_id UUID REFERENCES utilisateurs(id),
    date_validation TIMESTAMP,
    magasinier_id UUID REFERENCES utilisateurs(id),
    date_servie TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lignes_demande_magasin (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    demande_magasin_id UUID NOT NULL REFERENCES demandes_magasin(id) ON DELETE CASCADE,
    article_id UUID NOT NULL REFERENCES articles(id),
    quantite_demande INT NOT NULL CHECK (quantite_demande > 0),
    quantite_servie INT DEFAULT 0 CHECK (quantite_servie IS NULL OR (quantite_servie >= 0 AND quantite_servie <= quantite_demande)),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mouvements_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id UUID NOT NULL REFERENCES articles(id),
    type_mouvement VARCHAR(30) NOT NULL CHECK (type_mouvement IN ('ENTREE','SORTIE','AJUSTEMENT')),
    quantite INT NOT NULL CHECK (quantite >= 0),
    stock_anterieur INT NOT NULL CHECK (stock_anterieur >= 0),
    nouveau_stock INT NOT NULL CHECK (nouveau_stock >= 0),
    reference_document VARCHAR(100) NOT NULL,
    auteur_id UUID NOT NULL REFERENCES utilisateurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Séquence utilisée pour générer les numéros de bon de sortie (BS-AAAA-NNNN)
CREATE SEQUENCE IF NOT EXISTS seq_demande_magasin START 1;

-- Index des filtres des écrans et des clés étrangères
CREATE INDEX IF NOT EXISTS idx_mouvements_stock_article_date ON mouvements_stock (article_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_mouvements_stock_type ON mouvements_stock (type_mouvement);
CREATE INDEX IF NOT EXISTS idx_demandes_magasin_statut ON demandes_magasin (statut);
CREATE INDEX IF NOT EXISTS idx_demandes_magasin_demandeur ON demandes_magasin (demandeur_id);
CREATE INDEX IF NOT EXISTS idx_demandes_magasin_structure ON demandes_magasin (structure_code);
CREATE INDEX IF NOT EXISTS idx_demandes_magasin_service ON demandes_magasin (service_id);
CREATE INDEX IF NOT EXISTS idx_lignes_demande_magasin_demande ON lignes_demande_magasin (demande_magasin_id);
CREATE INDEX IF NOT EXISTS idx_lignes_demande_magasin_article ON lignes_demande_magasin (article_id);
