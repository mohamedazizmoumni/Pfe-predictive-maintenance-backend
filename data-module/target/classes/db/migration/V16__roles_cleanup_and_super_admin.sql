-- Ensure SUPER_ADMIN role exists
INSERT INTO roles (name, description)
VALUES ('SUPER_ADMIN', 'Super administrator with unrestricted platform access')
ON CONFLICT (name) DO NOTHING;

-- Ensure TECHNICIAN role exists for default account creation
INSERT INTO roles (name, description)
VALUES ('TECHNICIAN', 'Technician with operational access')
ON CONFLICT (name) DO NOTHING;

-- Give SUPER_ADMIN all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- Remove mappings for roles that are no longer used
DELETE FROM role_permissions rp
USING roles r
WHERE rp.role_id = r.id
  AND r.name IN ('VIEWER', 'DATA_SCIENTIST');

DELETE FROM user_roles ur
USING roles r
WHERE ur.role_id = r.id
  AND r.name IN ('VIEWER', 'DATA_SCIENTIST');

-- Remove roles from roles table
DELETE FROM roles
WHERE name IN ('VIEWER', 'DATA_SCIENTIST');
