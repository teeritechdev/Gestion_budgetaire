BEGIN;

CREATE TABLE directions (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    structure_code VARCHAR(20) NOT NULL REFERENCES structures(code),
    responsable_id UUID,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE services (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    direction_id UUID NOT NULL REFERENCES directions(id),
    responsable_id UUID,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

INSERT INTO directions (id, code, libelle, structure_code, responsable_id, actif, created_at, updated_at)
SELECT id, code, libelle, structure_code, responsable_id, actif, created_at, updated_at
FROM unites_organisationnelles
WHERE UPPER(type_structure) = 'DIRECTION';

DO $$
DECLARE
    nombre_orphelins BIGINT;
BEGIN
    WITH RECURSIVE ancestry(unit_id, ancestor_id, depth, visited) AS (
        SELECT unit.id,
               COALESCE(
                   (SELECT direction.id FROM directions direction WHERE direction.id = unit.direction_id),
                   unit.parent_id),
               1,
               ARRAY[unit.id]::UUID[]
        FROM unites_organisationnelles unit
        WHERE UPPER(unit.type_structure) <> 'DIRECTION'
          AND COALESCE(
              (SELECT direction.id FROM directions direction WHERE direction.id = unit.direction_id),
              unit.parent_id) IS NOT NULL
        UNION ALL
        SELECT ancestry.unit_id, parent.parent_id, ancestry.depth + 1, ancestry.visited || parent.id
        FROM ancestry
        JOIN unites_organisationnelles parent ON parent.id = ancestry.ancestor_id
        WHERE UPPER(parent.type_structure) <> 'DIRECTION'
          AND parent.parent_id IS NOT NULL
          AND NOT parent.id = ANY(ancestry.visited)
    ),
    mapped_directions AS (
        SELECT DISTINCT ON (ancestry.unit_id) ancestry.unit_id, direction.id AS direction_id
        FROM ancestry
        JOIN unites_organisationnelles candidate ON candidate.id = ancestry.ancestor_id
        JOIN directions direction ON direction.id = candidate.id
        ORDER BY ancestry.unit_id, ancestry.depth
    )
    SELECT COUNT(*)
    INTO nombre_orphelins
    FROM unites_organisationnelles unit
    LEFT JOIN mapped_directions mapped ON mapped.unit_id = unit.id
    WHERE UPPER(unit.type_structure) <> 'DIRECTION'
      AND mapped.direction_id IS NULL;

    IF nombre_orphelins > 0 THEN
        RAISE EXCEPTION
            'Migration interrompue : % unité(s) non-DIRECTION ne sont rattachées à aucune direction. Corrigez leur hiérarchie avant de relancer.',
            nombre_orphelins;
    END IF;
END $$;

WITH RECURSIVE ancestry(unit_id, ancestor_id, depth, visited) AS (
    SELECT unit.id,
           COALESCE(
               (SELECT direction.id FROM directions direction WHERE direction.id = unit.direction_id),
               unit.parent_id),
           1,
           ARRAY[unit.id]::UUID[]
    FROM unites_organisationnelles unit
    WHERE UPPER(unit.type_structure) <> 'DIRECTION'
      AND COALESCE(
          (SELECT direction.id FROM directions direction WHERE direction.id = unit.direction_id),
          unit.parent_id) IS NOT NULL
    UNION ALL
    SELECT ancestry.unit_id, parent.parent_id, ancestry.depth + 1, ancestry.visited || parent.id
    FROM ancestry
    JOIN unites_organisationnelles parent ON parent.id = ancestry.ancestor_id
    WHERE UPPER(parent.type_structure) <> 'DIRECTION'
      AND parent.parent_id IS NOT NULL
      AND NOT parent.id = ANY(ancestry.visited)
),
mapped_directions AS (
    SELECT DISTINCT ON (ancestry.unit_id) ancestry.unit_id, direction.id AS direction_id
    FROM ancestry
    JOIN unites_organisationnelles candidate ON candidate.id = ancestry.ancestor_id
    JOIN directions direction ON direction.id = candidate.id
    ORDER BY ancestry.unit_id, ancestry.depth
)
INSERT INTO services (id, code, libelle, direction_id, responsable_id, actif, created_at, updated_at)
SELECT unit.id, unit.code, unit.libelle, mapped.direction_id, unit.responsable_id, unit.actif, unit.created_at, unit.updated_at
FROM unites_organisationnelles unit
JOIN mapped_directions mapped ON mapped.unit_id = unit.id
WHERE UPPER(unit.type_structure) <> 'DIRECTION';

ALTER TABLE budget_allocation_structure
    ADD COLUMN structure_code VARCHAR(20);

UPDATE budget_allocation_structure allocation
SET structure_code = unit.structure_code
FROM unites_organisationnelles unit
WHERE allocation.structure_id = unit.id;

DO $$
DECLARE
    nombre_sans_structure BIGINT;
    contrainte RECORD;
BEGIN
    SELECT COUNT(*)
    INTO nombre_sans_structure
    FROM budget_allocation_structure
    WHERE structure_code IS NULL;

    IF nombre_sans_structure > 0 THEN
        RAISE EXCEPTION
            'Migration interrompue : % allocation(s) ne peuvent pas être rattachées à une Structure.',
            nombre_sans_structure;
    END IF;

    FOR contrainte IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'budget_allocation_structure'::regclass
          AND contype = 'f'
          AND conkey = ARRAY[(
              SELECT attnum
              FROM pg_attribute
              WHERE attrelid = 'budget_allocation_structure'::regclass
                AND attname = 'structure_id'
          )]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE budget_allocation_structure DROP CONSTRAINT %I', contrainte.conname);
    END LOOP;
END $$;

ALTER TABLE budget_allocation_structure
    DROP COLUMN structure_id,
    ALTER COLUMN structure_code SET NOT NULL,
    ADD CONSTRAINT fk_budget_allocation_structure_structure
        FOREIGN KEY (structure_code) REFERENCES structures(code);

COMMIT;
