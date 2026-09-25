CREATE TABLE playlist (
                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          CONSTRAINT playlist_name_not_blank
                              CHECK (length(trim(name)) > 0)
);