-- Une direction peut être rattachée à plusieurs structures (table de liaison direction_structures).
-- Le statut CSP disparaît de la base : une direction « CSP » est simplement rattachée à toutes les structures.
-- À exécuter après V20261030_02, application arrêtée, AVANT de démarrer la nouvelle version.
BEGIN;

CREATE TABLE IF NOT EXISTS direction_structures (
    direction_id   UUID        NOT NULL REFERENCES directions (id) ON DELETE CASCADE,
    structure_code VARCHAR(20) NOT NULL REFERENCES structures (code),
    PRIMARY KEY (direction_id, structure_code)
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'directions' AND column_name = 'structure_code') THEN
        -- Directions classiques : leur structure de rattachement actuelle.
        INSERT INTO direction_structures (direction_id, structure_code)
        SELECT id, structure_code FROM directions WHERE structure_code IS NOT NULL
        ON CONFLICT DO NOTHING;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'directions' AND column_name = 'est_csp') THEN
        -- Directions CSP : rattachées à toutes les structures existantes.
        INSERT INTO direction_structures (direction_id, structure_code)
        SELECT d.id, s.code FROM directions d CROSS JOIN structures s WHERE d.est_csp = TRUE
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

ALTER TABLE directions DROP CONSTRAINT IF EXISTS ck_directions_rattachement_csp;
ALTER TABLE directions DROP COLUMN IF EXISTS structure_code;
ALTER TABLE directions DROP COLUMN IF EXISTS est_csp;

COMMIT;
