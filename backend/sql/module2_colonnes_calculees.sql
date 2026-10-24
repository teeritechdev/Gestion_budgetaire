-- Colonnes calculées de budget_allocation_structure (MRD, Module 2).
-- A n'utilise ces colonnes qu'en lecture (insertable=false) : si la table a été créée par Hibernate
-- (ddl-auto=update), elles existent comme colonnes ordinaires et restent NULL. Ce script les remplace
-- par de vraies colonnes GENERATED, conformément au MRD. À exécuter une seule fois.

ALTER TABLE budget_allocation_structure DROP COLUMN IF EXISTS montant_alloue;
ALTER TABLE budget_allocation_structure
    ADD COLUMN montant_alloue NUMERIC(15,2) GENERATED ALWAYS AS (montant_vote + montant_modifie) STORED;

ALTER TABLE budget_allocation_structure DROP COLUMN IF EXISTS montant_disponible;
ALTER TABLE budget_allocation_structure
    ADD COLUMN montant_disponible NUMERIC(15,2) GENERATED ALWAYS AS
        ((montant_vote + montant_modifie) - (montant_engage + montant_liquide)) STORED;
