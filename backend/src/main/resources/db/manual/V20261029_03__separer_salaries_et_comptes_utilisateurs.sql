BEGIN;

ALTER TABLE utilisateurs RENAME TO utilisateurs_legacy;

CREATE TABLE salaries (
    id UUID PRIMARY KEY,
    matricule VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    sexe VARCHAR(20) NOT NULL DEFAULT 'NON_RENSEIGNE',
    telephone VARCHAR(30),
    email VARCHAR(150) NOT NULL UNIQUE,
    poste VARCHAR(100),
    date_embauche DATE NOT NULL,
    service_id UUID NOT NULL REFERENCES services(id),
    manager_id UUID REFERENCES salaries(id),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TEMPORARY TABLE migration_salaries AS
SELECT id AS utilisateur_id, gen_random_uuid() AS salarie_id
FROM utilisateurs_legacy;

INSERT INTO services (id, code, libelle, direction_id, actif)
SELECT gen_random_uuid(),
       'SVC-GEN-' || direction.id::text,
       'Service général - ' || direction.libelle,
       direction.id,
       TRUE
FROM utilisateurs_legacy utilisateur
JOIN unites_organisationnelles unite ON unite.id = utilisateur.structure_id
JOIN directions direction ON direction.id = unite.id
WHERE UPPER(unite.type_structure) = 'DIRECTION'
  AND NOT EXISTS (
      SELECT 1 FROM services service WHERE service.direction_id = direction.id
  );

DO $$
DECLARE
    nombre_sans_service BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO nombre_sans_service
    FROM utilisateurs_legacy utilisateur
    LEFT JOIN unites_organisationnelles unite ON unite.id = utilisateur.structure_id
    LEFT JOIN directions direction
        ON direction.id = unite.id AND UPPER(unite.type_structure) = 'DIRECTION'
    LEFT JOIN LATERAL (
        SELECT service.id
        FROM services service
        WHERE (direction.id IS NOT NULL AND service.direction_id = direction.id)
           OR (direction.id IS NULL AND service.id = unite.id)
        ORDER BY service.code
        LIMIT 1
    ) service_assigne ON TRUE
    WHERE service_assigne.id IS NULL;

    IF nombre_sans_service > 0 THEN
        RAISE EXCEPTION
            'Migration interrompue : % utilisateur(s) ne peuvent pas être rattachés à un Service.',
            nombre_sans_service;
    END IF;
END $$;

INSERT INTO salaries (
    id, matricule, nom, prenom, sexe, telephone, email, poste,
    date_embauche, service_id, manager_id, actif, created_at, updated_at
)
SELECT migration_salaries.salarie_id,
       utilisateur.matricule,
       utilisateur.nom,
       utilisateur.prenom,
       COALESCE(utilisateur.sexe, 'NON_RENSEIGNE'),
       utilisateur.telephone,
       utilisateur.email,
       utilisateur.role_metier,
       COALESCE(utilisateur.created_at::date, CURRENT_DATE),
       service_assigne.id,
       manager_salarie.salarie_id,
       utilisateur.actif,
       utilisateur.created_at,
       utilisateur.updated_at
FROM utilisateurs_legacy utilisateur
JOIN migration_salaries ON migration_salaries.utilisateur_id = utilisateur.id
JOIN unites_organisationnelles unite ON unite.id = utilisateur.structure_id
LEFT JOIN directions direction
    ON direction.id = unite.id AND UPPER(unite.type_structure) = 'DIRECTION'
JOIN LATERAL (
    SELECT service.id
    FROM services service
    WHERE (direction.id IS NOT NULL AND service.direction_id = direction.id)
       OR (direction.id IS NULL AND service.id = unite.id)
    ORDER BY service.code
    LIMIT 1
) service_assigne ON TRUE
LEFT JOIN migration_salaries manager_salarie
    ON manager_salarie.utilisateur_id = utilisateur.manager_id;

CREATE TABLE utilisateurs (
    id UUID PRIMARY KEY,
    username VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    salarie_id UUID NOT NULL UNIQUE REFERENCES salaries(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

INSERT INTO utilisateurs (
    id, username, mot_de_passe_hash, salarie_id, role_id, actif, created_at, updated_at
)
SELECT utilisateur.id,
       utilisateur.email,
       utilisateur.mot_de_passe_hash,
       migration_salaries.salarie_id,
       utilisateur.role_id,
       utilisateur.actif,
       utilisateur.created_at,
       utilisateur.updated_at
FROM utilisateurs_legacy utilisateur
JOIN migration_salaries ON migration_salaries.utilisateur_id = utilisateur.id;

DROP TABLE utilisateurs_legacy;
DROP TABLE migration_salaries;

COMMIT;
