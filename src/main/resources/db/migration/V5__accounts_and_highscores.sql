ALTER TABLE app_user ADD COLUMN password_hash VARCHAR(100);
CREATE TABLE game_highscore (
    user_id BIGINT PRIMARY KEY REFERENCES app_user(id),
    score INTEGER NOT NULL CHECK (score >= 0),
    achieved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
