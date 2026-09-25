CREATE TABLE movie (
                       tmdb_id BIGINT PRIMARY KEY CHECK (tmdb_id > 0)
);

CREATE TABLE playlist_movie (
                                playlist_id BIGINT NOT NULL REFERENCES playlist(id),
                                tmdb_id BIGINT NOT NULL REFERENCES movie(tmdb_id),
                                position INTEGER NOT NULL CHECK (position > 0),

                                PRIMARY KEY (playlist_id, tmdb_id),
                                UNIQUE (playlist_id, position)
);