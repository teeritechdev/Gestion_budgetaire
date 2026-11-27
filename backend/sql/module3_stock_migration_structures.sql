-- Migration d'une base créée avec l'ANCIEN module3_stock.sql (entite_code -> entites, service_id -> structures)
-- vers le modèle de Djibrila (structure_code -> structures, service_id -> services).
-- À exécuter UNE FOIS, après les migrations db/manual V20261029_* et V20261030_*. Rejouable sans effet.

BEGIN;

-- 1. entite_code devient structure_code (la table « entites » est devenue « structures » : la clé suit le renommage)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'demandes_magasin' AND column_name = 'entite_code') THEN
        ALTER TABLE demandes_magasin RENAME COLUMN entite_code TO structure_code;
    END IF;
END $$;

-- 2. service_id doit référencer services(id). Les anciennes valeurs désignaient des lignes de l'ancienne table
--    « structures » (devenue unites_organisationnelles) : on ne peut les conserver que si les mêmes identifiants
--    existent dans services ; sinon la migration s'arrête pour éviter de corrompre des données.
DO $$
DECLARE
    orphelines INT;
    contrainte TEXT;
BEGIN
    SELECT count(*) INTO orphelines FROM demandes_magasin d
     WHERE NOT EXISTS (SELECT 1 FROM services s WHERE s.id = d.service_id);
    IF orphelines > 0 THEN
        RAISE EXCEPTION '% demande(s) magasin référencent un service_id absent de la table services : les rattacher à un service avant de relancer.', orphelines;
    END IF;

    FOR contrainte IN
        SELECT conname FROM pg_constraint
         WHERE conrelid = 'demandes_magasin'::regclass AND contype = 'f'
           AND pg_get_constraintdef(oid) LIKE 'FOREIGN KEY (service_id)%'
    LOOP
        EXECUTE format('ALTER TABLE demandes_magasin DROP CONSTRAINT %I', contrainte);
    END LOOP;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_demandes_magasin_service') THEN
        ALTER TABLE demandes_magasin
            ADD CONSTRAINT fk_demandes_magasin_service FOREIGN KEY (service_id) REFERENCES services(id);
    END IF;
END $$;

-- 3. Index et contraintes de valeur (identiques à module3_stock.sql)
DROP INDEX IF EXISTS idx_demandes_magasin_entite;
CREATE INDEX IF NOT EXISTS idx_mouvements_stock_article_date ON mouvements_stock (article_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_mouvements_stock_type ON mouvements_stock (type_mouvement);
CREATE INDEX IF NOT EXISTS idx_demandes_magasin_statut ON demandes_magasin (statut);
CREATE INDEX IF NOT EXISTS idx_demandes_magasin_demandeur ON demandes_magasin (demandeur_id);
CREATE INDEX IF NOT EXISTS idx_demandes_magasin_structure ON demandes_magasin (structure_code);
CREATE INDEX IF NOT EXISTS idx_demandes_magasin_service ON demandes_magasin (service_id);
CREATE INDEX IF NOT EXISTS idx_lignes_demande_magasin_demande ON lignes_demande_magasin (demande_magasin_id);
CREATE INDEX IF NOT EXISTS idx_lignes_demande_magasin_article ON lignes_demande_magasin (article_id);

ALTER TABLE articles DROP CONSTRAINT IF EXISTS ck_articles_stock_positif;
ALTER TABLE articles ADD CONSTRAINT ck_articles_stock_positif CHECK (quantite_stock >= 0);
ALTER TABLE demandes_magasin DROP CONSTRAINT IF EXISTS ck_demandes_magasin_statut;
ALTER TABLE demandes_magasin ADD CONSTRAINT ck_demandes_magasin_statut
    CHECK (statut IN ('SOUMIS','VALIDE_N1','PARTIELLEMENT_SERVIE','SERVIE','REJETEE'));
ALTER TABLE lignes_demande_magasin DROP CONSTRAINT IF EXISTS ck_lignes_demande_quantites;
ALTER TABLE lignes_demande_magasin ADD CONSTRAINT ck_lignes_demande_quantites
    CHECK (quantite_demande > 0 AND (quantite_servie IS NULL OR (quantite_servie >= 0 AND quantite_servie <= quantite_demande)));
ALTER TABLE mouvements_stock DROP CONSTRAINT IF EXISTS ck_mouvements_stock_type;
ALTER TABLE mouvements_stock ADD CONSTRAINT ck_mouvements_stock_type CHECK (type_mouvement IN ('ENTREE','SORTIE','AJUSTEMENT'));

COMMIT;
