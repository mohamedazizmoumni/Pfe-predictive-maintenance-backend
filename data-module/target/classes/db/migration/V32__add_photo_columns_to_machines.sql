-- V32: Add photo-related columns to machines table
-- Purpose: Support machine photo storage and metadata

-- Add photo_path column to machines table
ALTER TABLE machines 
ADD COLUMN IF NOT EXISTS photo_path VARCHAR(500);

-- Add photo_content_type column to machines table
ALTER TABLE machines 
ADD COLUMN IF NOT EXISTS photo_content_type VARCHAR(100);

-- Add comment for documentation
COMMENT ON COLUMN machines.photo_path IS 'Path to the machine photo file';
COMMENT ON COLUMN machines.photo_content_type IS 'MIME type of the photo file (e.g., image/jpeg, image/png)';
