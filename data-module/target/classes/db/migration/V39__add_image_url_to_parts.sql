-- Add image_url column to parts table for storing part photos

ALTER TABLE parts ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

-- Add comment
COMMENT ON COLUMN parts.image_url IS 'URL or path to the part image/photo';
