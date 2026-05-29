-- V31: Add category and subCategory columns to machines table
-- Purpose: Support machine categorization for better organization and filtering

-- Add category column to machines table
ALTER TABLE machines 
ADD COLUMN IF NOT EXISTS category VARCHAR(100);

-- Add sub_category column to machines table
ALTER TABLE machines 
ADD COLUMN IF NOT EXISTS sub_category VARCHAR(100);

-- Create index for category queries
CREATE INDEX IF NOT EXISTS idx_machines_category ON machines(category);

-- Create index for sub_category queries
CREATE INDEX IF NOT EXISTS idx_machines_sub_category ON machines(sub_category);

-- Add comment for documentation
COMMENT ON COLUMN machines.category IS 'Machine category for classification and filtering';
COMMENT ON COLUMN machines.sub_category IS 'Machine sub-category for detailed classification';
