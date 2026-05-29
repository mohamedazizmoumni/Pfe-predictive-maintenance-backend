-- Manual script to create categories if migration V38 hasn't run yet
-- Run this in your PostgreSQL database

-- Check if categories exist
SELECT COUNT(*) as category_count FROM categories;

-- If count is 0, insert default categories
INSERT INTO categories (name) VALUES
('Bearings'),
('Belts'),
('Motors'),
('Sensors'),
('Filters'),
('Lubricants'),
('Electrical'),
('Hydraulic'),
('Pneumatic'),
('Mechanical'),
('Safety Equipment'),
('Tools'),
('Fasteners'),
('Seals'),
('Gaskets')
ON CONFLICT (name) DO NOTHING;

-- Verify categories were created
SELECT * FROM categories ORDER BY name;
