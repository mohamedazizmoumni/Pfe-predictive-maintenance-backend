

INSERT INTO roles (name, description) VALUES
('ADMIN', 'Administrator with full system access'),
('MANAGER', 'Manager with supervisory capabilities'),
('TECHNICIAN', 'Technician with operational access'),
('DATA_SCIENTIST', 'Data scientist with analytics access'),
('VIEWER', 'Read-only access to reports')
ON CONFLICT (name) DO NOTHING;

-- Insert default permissions
INSERT INTO permissions (name, description) VALUES
('VIEW_DASHBOARD', 'Can view dashboard'),
('MANAGE_USERS', 'Can manage users'),
('MANAGE_MACHINES', 'Can manage machines'),
('VIEW_MACHINES', 'Can view machines'),
('MANAGE_MAINTENANCE', 'Can manage maintenance tasks'),
('VIEW_MAINTENANCE', 'Can view maintenance tasks'),
('MANAGE_INVENTORY', 'Can manage inventory'),
('VIEW_INVENTORY', 'Can view inventory'),
('RUN_PREDICTIONS', 'Can run ML predictions'),
('VIEW_PREDICTIONS', 'Can view predictions'),
('MANAGE_ALERTS', 'Can manage alerts'),
('VIEW_ALERTS', 'Can view alerts'),
('EXPORT_DATA', 'Can export data'),
('VIEW_REPORTS', 'Can view reports'),
('SYSTEM_CONFIG', 'Can modify system configuration')
ON CONFLICT (name) DO NOTHING;

-- Assign permissions to ADMIN role (all permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Assign permissions to MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'MANAGER' AND p.name IN (
    'VIEW_DASHBOARD', 'VIEW_MACHINES', 'MANAGE_MAINTENANCE', 
    'VIEW_MAINTENANCE', 'VIEW_INVENTORY', 'VIEW_ALERTS', 'VIEW_REPORTS'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to TECHNICIAN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'TECHNICIAN' AND p.name IN (
    'VIEW_DASHBOARD', 'VIEW_MACHINES', 'VIEW_MAINTENANCE', 
    'VIEW_INVENTORY', 'VIEW_ALERTS', 'EXPORT_DATA'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to DATA_SCIENTIST role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'DATA_SCIENTIST' AND p.name IN (
    'VIEW_DASHBOARD', 'RUN_PREDICTIONS', 'VIEW_PREDICTIONS', 
    'VIEW_MACHINES', 'VIEW_ALERTS', 'EXPORT_DATA', 'VIEW_REPORTS'
)
ON CONFLICT DO NOTHING;

-- Assign permissions to VIEWER role (minimal)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'VIEWER' AND p.name IN (
    'VIEW_DASHBOARD', 'VIEW_MACHINES', 'VIEW_INVENTORY', 
    'VIEW_ALERTS', 'VIEW_REPORTS'
)
ON CONFLICT DO NOTHING;
