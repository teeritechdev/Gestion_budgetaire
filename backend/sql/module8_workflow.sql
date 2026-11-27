-- ==============================================================================
-- MODULE 8 : MOTEUR DE WORKFLOW TRANSVERSAL & CIRCUITS DE VALIDATION
-- Tables : workflow_definitions, workflow_etapes, workflow_instances
-- ==============================================================================

-- 1. Table des modèles de processus (workflow_definitions)
CREATE TABLE IF NOT EXISTS workflow_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    description TEXT,
    type_entite VARCHAR(50) NOT NULL,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Table des étapes et graphe de décision (workflow_etapes)
CREATE TABLE IF NOT EXISTS workflow_etapes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    num_etape INT NOT NULL,
    code_etape VARCHAR(50) NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    permission_requise VARCHAR(100),
    role_requis VARCHAR(50),
    montant_minimum NUMERIC(15,2) DEFAULT 0,
    montant_maximum NUMERIC(15,2),
    etape_suivante_succes_id UUID REFERENCES workflow_etapes(id) ON DELETE SET NULL,
    etape_suivante_rejet_id UUID REFERENCES workflow_etapes(id) ON DELETE SET NULL,
    est_etape_finale BOOLEAN DEFAULT FALSE,
    est_etape_rejet BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workflow_num_etape UNIQUE (workflow_id, num_etape)
);

-- 3. Table des instances en cours d'exécution (workflow_instances)
CREATE TABLE IF NOT EXISTS workflow_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflow_definitions(id),
    dossier_id UUID NOT NULL,
    type_entite VARCHAR(50) NOT NULL,
    reference_dossier VARCHAR(100) NOT NULL,
    objet VARCHAR(255),
    montant_total NUMERIC(15,2) DEFAULT 0,
    demandeur_id UUID REFERENCES utilisateurs(id),
    etape_courante_id UUID REFERENCES workflow_etapes(id),
    statut_actuel VARCHAR(50) DEFAULT 'EN_ATTENTE_VALIDATION',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour les performances de recherche
CREATE INDEX IF NOT EXISTS idx_wf_def_code ON workflow_definitions(code);
CREATE INDEX IF NOT EXISTS idx_wf_def_type ON workflow_definitions(type_entite);
CREATE INDEX IF NOT EXISTS idx_wf_etapes_wf ON workflow_etapes(workflow_id);
CREATE INDEX IF NOT EXISTS idx_wf_instances_dossier ON workflow_instances(dossier_id);
CREATE INDEX IF NOT EXISTS idx_wf_instances_statut ON workflow_instances(statut_actuel);
CREATE INDEX IF NOT EXISTS idx_wf_instances_etape ON workflow_instances(etape_courante_id);

-- Migration rétrocompatible pour historique_validations (ajout de instance_id)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'historique_validations' AND column_name = 'instance_id'
    ) THEN
        ALTER TABLE historique_validations ADD COLUMN instance_id UUID REFERENCES workflow_instances(id) ON DELETE CASCADE;
        CREATE INDEX IF NOT EXISTS idx_hist_val_instance ON historique_validations(instance_id);
    END IF;
END $$;

-- Insertion du Workflow d'Engagement P2P par défaut
INSERT INTO workflow_definitions (id, code, libelle, description, type_entite, actif)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'CHAINE_ENGAGEMENT_P2P',
    'Chaîne complète d’engagement et achat (P2P)',
    'Circuit d’approbation des demandes d’achats avec contrôle hiérarchique N+1, vérification budgétaire, validation DFI et arbitrage DG pour les dossiers de plus de 10 millions FCFA.',
    'DEMANDE_ACHAT',
    TRUE
) ON CONFLICT (code) DO NOTHING;

-- Insertion des 4 étapes du Workflow P2P
INSERT INTO workflow_etapes (id, workflow_id, num_etape, code_etape, libelle, permission_requise, role_requis, montant_minimum, montant_maximum, est_etape_finale, est_etape_rejet)
VALUES 
(
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    1,
    'ETP_VISA_N1',
    'Visa du Manager N+1',
    'demande_achat.valider',
    'ROLE_MANAGER',
    0,
    NULL,
    FALSE,
    FALSE
),
(
    'b0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    2,
    'ETP_CONTROLE_BUDGET',
    'Contrôle de Disponibilité Budgétaire',
    'demande_achat.valider',
    'ROLE_CONTROLEUR_GESTION',
    0,
    NULL,
    FALSE,
    FALSE
),
(
    'b0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    3,
    'ETP_VAL_DFI',
    'Validation Direction Financière & Investissements (DFI)',
    'demande_achat.valider',
    'ROLE_DFI',
    0,
    NULL,
    FALSE,
    FALSE
),
(
    'b0000000-0000-0000-0000-000000000004',
    'a0000000-0000-0000-0000-000000000001',
    4,
    'ETP_APP_DG',
    'Approbation Direction Générale (Seuil > 10M FCFA)',
    'demande_achat.valider',
    'ROLE_DG',
    10000000.00,
    NULL,
    TRUE,
    FALSE
)
ON CONFLICT (workflow_id, num_etape) DO NOTHING;

-- Configuration des liens de transition (Graphe)
UPDATE workflow_etapes SET etape_suivante_succes_id = 'b0000000-0000-0000-0000-000000000002' WHERE id = 'b0000000-0000-0000-0000-000000000001';
UPDATE workflow_etapes SET etape_suivante_succes_id = 'b0000000-0000-0000-0000-000000000003' WHERE id = 'b0000000-0000-0000-0000-000000000002';
UPDATE workflow_etapes SET etape_suivante_succes_id = 'b0000000-0000-0000-0000-000000000004' WHERE id = 'b0000000-0000-0000-0000-000000000003';

