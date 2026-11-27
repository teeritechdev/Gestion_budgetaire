-- Nouvelle règle : une direction est rattachée à UNE seule structure (colonne directions.structure_code).
-- Reprise des données : la structure est lue dans direction_structures ; pour une direction liée à
-- plusieurs structures, la première par ordre de code est retenue (à corriger ensuite dans l'application).
-- La table direction_structures est conservée (plus utilisée) : la supprimer une fois la reprise vérifiée.
BEGIN;

ALTER TABLE directions ADD COLUMN IF NOT EXISTS structure_code VARCHAR(20);

UPDATE directions d
SET structure_code = (SELECT MIN(ds.structure_code) FROM direction_structures ds WHERE ds.direction_id = d.id)
WHERE d.structure_code IS NULL;

-- Direction sans aucune structure (ne devrait pas exister) : rattachée à la première structure.
UPDATE directions SET structure_code = (SELECT MIN(code) FROM structures) WHERE structure_code IS NULL;

ALTER TABLE directions ALTER COLUMN structure_code SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_directions_structure') THEN
        ALTER TABLE directions ADD CONSTRAINT fk_directions_structure
            FOREIGN KEY (structure_code) REFERENCES structures (code);
    END IF;
END $$;

COMMIT;

-- Après vérification :
-- DROP TABLE direction_structures;
