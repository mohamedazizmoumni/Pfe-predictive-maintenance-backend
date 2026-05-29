-- Add profile picture URL column to users table
ALTER TABLE users ADD COLUMN profile_picture_url TEXT;
-- Add index for faster lookups if needed
CREATE INDEX idx_users_profile_picture ON users(id);
