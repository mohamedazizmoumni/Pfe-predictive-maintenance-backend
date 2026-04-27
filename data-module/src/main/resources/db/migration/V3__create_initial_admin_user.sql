-- V3__create_initial_admin_user.sql
-- Create initial admin user

-- Insert admin user (password should be changed in production)
-- Default password is 'admin' (this should be changed!)
INSERT INTO users (username, email, password, first_name, last_name, display_name, department, status)
VALUES ('admin', 'admin@predictive-maintenance.local', '$2a$10$slYQmyNdGzin7olVN3p72OPST9/PgBkqquzi.Oy1g7WzxQYDGM1Ey', 'System', 'Administrator', 'System Admin', 'IT', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
