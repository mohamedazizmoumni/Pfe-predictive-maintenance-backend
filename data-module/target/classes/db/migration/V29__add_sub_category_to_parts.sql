-- Add sub_category column to parts table if it doesn't exist
ALTER TABLE parts ADD COLUMN IF NOT EXISTS sub_category VARCHAR(100);

-- Create index for sub_category queries
CREATE INDEX IF NOT EXISTS idx_parts_sub_category ON parts(sub_category);
