-- Module 8 : Moteur de workflow générique (conforme au MRD, BDD_Budget.pdf, pages 67 à 69)
-- Aucune dépendance sur les autres modules : le moteur ne connaît que des dossier_id (UUID).

CREATE TABLE IF NOT EXISTS workflow_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workflow_etapes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    num_etape INT NOT NULL,
    code_etape VARCHAR(50) NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    permission_requise VARCHAR(100) NOT NULL,
    montant_minimum NUMERIC(15,2),
    montant_maximum NUMERIC(15,2),
    etape_suivante_succes_id UUID REFERENCES workflow_etapes(id),
    etape_suivante_rejet_id UUID REFERENCES workflow_etapes(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workflow_id, code_etape)
);

CREATE TABLE IF NOT EXISTS workflow_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflow_definitions(id),
    dossier_id UUID NOT NULL,
    etape_courante_id UUID REFERENCES workflow_etapes(id),   -- NULL une fois le circuit terminé
    statut_actuel VARCHAR(30) NOT NULL,                       -- EN_ATTENTE_VALIDATION, APPROUVE, REJETE, ANNULE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workflow_id, dossier_id)
);

CREATE INDEX IF NOT EXISTS idx_workflow_instances_dossier ON workflow_instances(dossier_id);
