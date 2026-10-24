-- Module 4 : quantités décimales (PDF : quantite et quantiteCommandee en BigDecimal).
-- À appliquer sur une base existante ; module4_achats.sql est déjà à jour pour une base neuve.
-- montant_total_ht est une colonne générée qui dépend de quantite : il faut la recréer.
BEGIN;
ALTER TABLE lignes_demande_achat DROP COLUMN montant_total_ht;
ALTER TABLE lignes_demande_achat ALTER COLUMN quantite TYPE NUMERIC(15,3);
ALTER TABLE lignes_demande_achat
    ADD COLUMN montant_total_ht NUMERIC(15,2) GENERATED ALWAYS AS (ROUND(quantite * prix_unitaire_estime, 2)) STORED;
ALTER TABLE lignes_bon_commande ALTER COLUMN quantite_commandee TYPE NUMERIC(15,3);
COMMIT;
