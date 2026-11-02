ALTER TABLE repartition_csp_rules
    ADD COLUMN IF NOT EXISTS allocation_structure_id UUID;
