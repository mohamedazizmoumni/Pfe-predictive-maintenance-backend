-- Track whether a user has completed first-time face enrollment.
-- face_enrolled    : false until the user captures their face after first login.
-- face_enrolled_at : timestamp of the successful enrollment (null until enrolled).

ALTER TABLE users ADD COLUMN IF NOT EXISTS face_enrolled      BOOLEAN   NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS face_enrolled_at   TIMESTAMP;

-- Existing users who already have a profile_picture_url are treated as enrolled
-- so they are not prompted again on their next login.
UPDATE users
SET    face_enrolled    = TRUE,
       face_enrolled_at = CURRENT_TIMESTAMP
WHERE  profile_picture_url IS NOT NULL
  AND  profile_picture_url <> '';
