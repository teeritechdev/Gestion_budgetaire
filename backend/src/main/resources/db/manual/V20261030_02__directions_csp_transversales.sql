-- Une direction CSP sert toutes les structures du groupe : elle n'est rattachée à aucune structure.
-- À exécuter après V20261030_01, application arrêtée.
BEGIN;

ALTER TABLE directions ALTER COLUMN structure_code DROP NOT NULL;

-- Les directions CSP deviennent transversales.
UPDATE directions SET structure_code = NULL WHERE est_csp = TRUE;

-- Cohérence : une direction classique a toujours une structure, une direction CSP n'en a pas.
ALTER TABLE directions DROP CONSTRAINT IF EXISTS ck_directions_rattachement_csp;
ALTER TABLE directions ADD CONSTRAINT ck_directions_rattachement_csp
    CHECK ((est_csp = TRUE AND structure_code IS NULL) OR (est_csp = FALSE AND structure_code IS NOT NULL));

COMMIT;
