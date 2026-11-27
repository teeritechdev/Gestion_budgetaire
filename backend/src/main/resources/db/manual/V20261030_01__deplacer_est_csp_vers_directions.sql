-- Le statut CSP (centre de services partagés) passe de la Structure à la Direction.
-- Reprise des données : toutes les directions d'une structure qui était CSP deviennent CSP,
-- ce qui conserve à l'identique le comportement du module Répartition CSP
-- (une structure « héberge un CSP » dès qu'une de ses directions actives est CSP).
-- À exécuter une fois, application arrêtée, AVANT de démarrer la nouvelle version.
BEGIN;

ALTER TABLE directions ADD COLUMN IF NOT EXISTS est_csp BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'structures' AND column_name = 'est_csp') THEN
        UPDATE directions d
           SET est_csp = TRUE
          FROM structures s
         WHERE d.structure_code = s.code
           AND s.est_csp = TRUE;

        ALTER TABLE structures DROP COLUMN est_csp;
    END IF;
END $$;

COMMIT;
