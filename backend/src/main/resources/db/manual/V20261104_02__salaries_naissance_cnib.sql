-- Salariés : date et lieu de naissance, numéro CNIB (unique, B suivi de chiffres).
-- Obligatoires pour toute création ou modification (contrôle applicatif) ; nullables en base
-- pour ne pas rejeter les dossiers existants, qui seront complétés à leur prochaine modification.
BEGIN;

ALTER TABLE salaries ADD COLUMN IF NOT EXISTS date_naissance DATE;
ALTER TABLE salaries ADD COLUMN IF NOT EXISTS lieu_naissance VARCHAR(100);
ALTER TABLE salaries ADD COLUMN IF NOT EXISTS numero_cnib VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS ux_salaries_numero_cnib ON salaries (numero_cnib) WHERE numero_cnib IS NOT NULL;

ALTER TABLE salaries DROP CONSTRAINT IF EXISTS ck_salaries_numero_cnib;
ALTER TABLE salaries ADD CONSTRAINT ck_salaries_numero_cnib CHECK (numero_cnib IS NULL OR numero_cnib ~ '^B[0-9]{5,15}$');

COMMIT;
