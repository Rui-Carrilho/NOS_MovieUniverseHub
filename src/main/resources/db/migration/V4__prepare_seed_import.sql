ALTER TABLE playlist
    ADD COLUMN source_id VARCHAR(100) UNIQUE;

CREATE TABLE user_rating (
                             user_id BIGINT NOT NULL REFERENCES app_user(id),
                             tmdb_id BIGINT NOT NULL REFERENCES movie(tmdb_id),
                             stars SMALLINT NOT NULL CHECK (stars BETWEEN 1 AND 10),
                             rated_on DATE NOT NULL,

                             PRIMARY KEY (user_id, tmdb_id)
);

CREATE TABLE seed_import (
                             checksum CHAR(64) PRIMARY KEY,
                             imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);