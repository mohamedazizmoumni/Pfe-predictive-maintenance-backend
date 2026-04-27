-- Ensure SUPER_ADMIN role exists and has all permissions
INSERT INTO roles (name, description)
VALUES ('SUPER_ADMIN', 'Super administrator with unrestricted platform access')
ON CONFLICT (name) DO NOTHING;

-- Grant every permission to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- If seeded superadmin user exists, ensure SUPER_ADMIN role is assigned
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'SUPER_ADMIN'
WHERE u.username = 'superadmin'
ON CONFLICT DO NOTHING;
