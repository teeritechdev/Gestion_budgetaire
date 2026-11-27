WITH repartition_totaux AS (
    SELECT
        id,
        SUM(valeur_cle) OVER (
            PARTITION BY activite_csp_id, activite_beneficiaire_id, cle_type
        ) AS total
    FROM repartition_csp_rules
    WHERE valeur_cle IS NOT NULL
      AND valeur_cle >= 0
)
UPDATE repartition_csp_rules AS rule
SET valeur_cle = ROUND(rule.valeur_cle / 100.0, 4)
FROM repartition_totaux
WHERE rule.id = repartition_totaux.id
  AND repartition_totaux.total = 100;
