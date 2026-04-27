-- V10__create_superadmin_user.sql
-- Create superadmin user with access to all roles

-- Insert superadmin user (password: superadmin - change in production!)
INSERT INTO users (username, email, password, first_name, last_name, display_name, department, status)
VALUES ('superadmin', 'superadmin@predictive-maintenance.local', '$2a$10$slYQmyNdGzin7olVN3p72OPST9/PgBkqquzi.Oy1g7WzxQYDGM1Ey', 'Super', 'Administrator', 'Super Admin', 'IT', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- Assign ALL roles to superadmin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'superadmin'
ON CONFLICT DO NOTHING;
