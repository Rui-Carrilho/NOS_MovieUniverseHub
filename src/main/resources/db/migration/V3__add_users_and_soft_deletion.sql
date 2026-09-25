CREATE TABLE app_user (
                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          username VARCHAR(80) NOT NULL UNIQUE,
                          CHECK (length(trim(username)) > 0)
);

ALTER TABLE playlist
    ADD COLUMN user_id BIGINT REFERENCES app_user(id),
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing practice playlists need an owner.
-- Create this account only when there are existing playlists.
INSERT INTO app_user (username)
SELECT 'learner'
    WHERE EXISTS (SELECT 1 FROM playlist);

UPDATE playlist
SET user_id = (
    SELECT id
    FROM app_user
    WHERE username = 'learner'
)
WHERE user_id IS NULL;

ALTER TABLE playlist
    ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX playlist_user_id_idx ON playlist(user_id);