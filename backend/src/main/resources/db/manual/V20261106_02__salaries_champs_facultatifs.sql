-- Salariés : l'e-mail professionnel et la date d'embauche deviennent facultatifs
-- (comme la date et le lieu de naissance et le numéro CNIB). L'unicité de l'e-mail est conservée.
ALTER TABLE salaries ALTER COLUMN email DROP NOT NULL;
ALTER TABLE salaries ALTER COLUMN date_embauche DROP NOT NULL;
