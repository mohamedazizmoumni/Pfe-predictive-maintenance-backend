-- Seed default inventory categories for parts management

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
