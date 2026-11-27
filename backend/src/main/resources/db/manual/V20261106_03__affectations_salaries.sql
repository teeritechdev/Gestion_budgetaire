-- Affectations des salariés : un salarié peut être affecté à une ou plusieurs structures, directions ou services.
-- Le service du dossier salarié (salaries.service_id) est repris comme affectation principale, puis devient
-- facultatif : l'application ne l'utilise plus (colonne à supprimer une fois la reprise vérifiée).
BEGIN;

CREATE TABLE IF NOT EXISTS affectations (
    id             UUID          NOT NULL PRIMARY KEY,
    salarie_id     UUID          NOT NULL REFERENCES salaries (id),
    niveau         VARCHAR(20)   NOT NULL CHECK (niveau IN ('STRUCTURE', 'DIRECTION', 'SERVICE')),
    structure_code VARCHAR(20)   REFERENCES structures (code),
    direction_id   UUID          REFERENCES directions (id),
    service_id     UUID          REFERENCES services (id),
    fonction       VARCHAR(100),
    principale     BOOLEAN       NOT NULL DEFAULT FALSE,
    date_debut     DATE          NOT NULL,
    date_fin       DATE,
    commentaire    VARCHAR(500),
    created_at     TIMESTAMP(6),
    updated_at     TIMESTAMP(6),
    -- Exactement la cible correspondant au niveau.
    CONSTRAINT ck_affectations_cible CHECK (
        (niveau = 'STRUCTURE' AND structure_code IS NOT NULL AND direction_id IS NULL AND service_id IS NULL)
     OR (niveau = 'DIRECTION' AND direction_id IS NOT NULL AND structure_code IS NULL AND service_id IS NULL)
     OR (niveau = 'SERVICE'   AND service_id IS NOT NULL AND structure_code IS NULL AND direction_id IS NULL)),
    CONSTRAINT ck_affectations_periode CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE INDEX IF NOT EXISTS idx_affectations_salarie ON affectations (salarie_id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'salaries' AND column_name = 'service_id') THEN
        INSERT INTO affectations (id, salarie_id, niveau, service_id, fonction, principale, date_debut, created_at, updated_at)
        SELECT gen_random_uuid(), s.id, 'SERVICE', s.service_id, s.poste, TRUE,
               COALESCE(s.date_embauche, s.created_at::date, CURRENT_DATE), NOW(), NOW()
        FROM salaries s
        WHERE s.service_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM affectations a WHERE a.salarie_id = s.id);

        ALTER TABLE salaries ALTER COLUMN service_id DROP NOT NULL;
    END IF;
END $$;

COMMIT;

-- Après vérification :
-- ALTER TABLE salaries DROP COLUMN service_id;
