CREATE TABLE IF NOT EXISTS agi_organization (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    code VARCHAR(100) NOT NULL,
    external_org_id VARCHAR(200),
    name VARCHAR(200) NOT NULL,
    org_type VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64),
    path VARCHAR(1000) NOT NULL,
    level INTEGER NOT NULL,
    sort_order INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_agi_organization_parent FOREIGN KEY (parent_id) REFERENCES agi_organization(id)
);

INSERT INTO agi_organization (
    id, tenant_id, code, external_org_id, name, org_type, parent_id, path, level, sort_order, status, created_at, updated_at
)
SELECT
    'org_' || code,
    tenant_id,
    code,
    external_unit_id,
    name,
    CASE WHEN unit_type = 'PLATFORM' THEN 'UNIT' ELSE 'UNIT' END,
    NULL,
    '/' || 'org_' || code,
    1,
    0,
    status,
    created_at,
    updated_at
FROM agi_unit
WHERE NOT EXISTS (
    SELECT 1 FROM agi_organization existing WHERE existing.id = 'org_' || agi_unit.code
);

INSERT INTO agi_organization (
    id, tenant_id, code, external_org_id, name, org_type, parent_id, path, level, sort_order, status, created_at, updated_at
)
SELECT
    'org_' || department.code,
    department.tenant_id,
    department.code,
    department.external_department_id,
    department.name,
    'DEPARTMENT',
    CASE
        WHEN department.parent_id IS NULL THEN unit_org.id
        ELSE parent_org.id
    END,
    CASE
        WHEN department.parent_id IS NULL THEN unit_org.path || '/' || 'org_' || department.code
        ELSE parent_org.path || '/' || 'org_' || department.code
    END,
    CASE
        WHEN department.parent_id IS NULL THEN 2
        ELSE parent_org.level + 1
    END,
    department.sort_order,
    department.status,
    department.created_at,
    department.updated_at
FROM agi_department department
JOIN agi_unit unit_source ON unit_source.id = department.unit_id
JOIN agi_organization unit_org ON unit_org.id = 'org_' || unit_source.code
LEFT JOIN agi_department parent_source ON parent_source.id = department.parent_id
LEFT JOIN agi_organization parent_org ON parent_org.id = 'org_' || parent_source.code
WHERE NOT EXISTS (
    SELECT 1 FROM agi_organization existing WHERE existing.id = 'org_' || department.code
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agi_organization_tenant_code ON agi_organization(tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_agi_organization_parent ON agi_organization(tenant_id, parent_id, sort_order);

CREATE TABLE IF NOT EXISTS agi_user_organization (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES agi_tenant(id),
    user_id VARCHAR(64) NOT NULL REFERENCES agi_user(id) ON DELETE CASCADE,
    organization_id VARCHAR(64) NOT NULL REFERENCES agi_organization(id),
    primary_organization BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO agi_user_organization (id, tenant_id, user_id, organization_id, primary_organization, created_at)
SELECT
    REPLACE(agi_user_unit.id, '_unit_', '_org_'),
    agi_user_unit.tenant_id,
    agi_user_unit.user_id,
    organization.id,
    primary_unit,
    agi_user_unit.created_at
FROM agi_user_unit
JOIN agi_unit unit_source ON unit_source.id = agi_user_unit.unit_id
JOIN agi_organization organization ON organization.id = 'org_' || unit_source.code
WHERE NOT EXISTS (
    SELECT 1 FROM agi_user_organization existing WHERE existing.id = REPLACE(agi_user_unit.id, '_unit_', '_org_')
);

INSERT INTO agi_user_organization (id, tenant_id, user_id, organization_id, primary_organization, created_at)
SELECT
    REPLACE(agi_user_department.id, '_dept_', '_org_'),
    agi_user_department.tenant_id,
    agi_user_department.user_id,
    organization.id,
    FALSE,
    agi_user_department.created_at
FROM agi_user_department
JOIN agi_department department_source ON department_source.id = agi_user_department.department_id
JOIN agi_organization organization ON organization.id = 'org_' || department_source.code
WHERE NOT EXISTS (
    SELECT 1 FROM agi_user_organization existing WHERE existing.id = REPLACE(agi_user_department.id, '_dept_', '_org_')
);

CREATE INDEX IF NOT EXISTS idx_agi_user_organization_user ON agi_user_organization(user_id, organization_id);

ALTER TABLE agi_role ADD COLUMN IF NOT EXISTS organization_id VARCHAR(64);
UPDATE agi_role
SET organization_id = (
    SELECT organization.id
    FROM agi_unit unit_source
    JOIN agi_organization organization ON organization.id = 'org_' || unit_source.code
    WHERE unit_source.id = agi_role.unit_id
)
WHERE unit_id IS NOT NULL AND organization_id IS NULL;
ALTER TABLE agi_role DROP CONSTRAINT IF EXISTS fk_agi_role_unit;
ALTER TABLE agi_role DROP COLUMN IF EXISTS unit_id;
ALTER TABLE agi_role ADD CONSTRAINT fk_agi_role_organization FOREIGN KEY (organization_id) REFERENCES agi_organization(id);

DROP TABLE IF EXISTS agi_user_department;
DROP TABLE IF EXISTS agi_user_unit;
DROP TABLE IF EXISTS agi_department;
DROP TABLE IF EXISTS agi_unit;
