-- Module 3 : Référentiels Articles & Stock (conforme au MRD, BDD_Budget.pdf)
-- Prérequis : tables du Module 1 (utilisateurs, structures, entites) déjà créées.

CREATE TABLE IF NOT EXISTS articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_article VARCHAR(50) UNIQUE NOT NULL,
    designation VARCHAR(255) NOT NULL,
    description TEXT,
    unite_mesure VARCHAR(30) NOT NULL,
    quantite_stock INT DEFAULT 0,
    seuil_reapprovisionnement INT DEFAULT 0,
    prix_unitaire_moyen_pondere NUMERIC(15,2) DEFAULT 0.00,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS demandes_magasin (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_demande VARCHAR(50) UNIQUE NOT NULL,
    demandeur_id UUID NOT NULL REFERENCES utilisateurs(id),
    service_id UUID NOT NULL REFERENCES structures(id),
    entite_code VARCHAR(20) NOT NULL REFERENCES entites(code),
    objet VARCHAR(255) NOT NULL,
    statut VARCHAR(30) DEFAULT 'SOUMIS',
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
    quantite_demande INT NOT NULL,
    quantite_servie INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mouvements_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id UUID NOT NULL REFERENCES articles(id),
    type_mouvement VARCHAR(30) NOT NULL,
    quantite INT NOT NULL,
    stock_anterieur INT NOT NULL,
    nouveau_stock INT NOT NULL,
    reference_document VARCHAR(100) NOT NULL,
    auteur_id UUID NOT NULL REFERENCES utilisateurs(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Séquence utilisée pour générer les numéros de bon de sortie (BS-AAAA-NNNN)
CREATE SEQUENCE IF NOT EXISTS seq_demande_magasin START 1;
