package com.movieuniverse.hub.playlist;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PlaylistMovieRepository {

    private final JdbcTemplate jdbc;

    public PlaylistMovieRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean lockPlaylist(long playlistId) {
        List<Long> ids = jdbc.query(
                """
                SELECT id
                FROM playlist
                WHERE id = ? AND deleted = FALSE
                FOR UPDATE
                """,
                (row, rowNumber) -> row.getLong("id"),
                playlistId
        );

        return !ids.isEmpty();
    }

    public List<MovieMembership> findAll(long playlistId) {
        return jdbc.query(
                """
                SELECT tmdb_id, position
                FROM playlist_movie
                WHERE playlist_id = ?
                ORDER BY position
                """,
                (row, rowNumber) -> new MovieMembership(
                        row.getLong("tmdb_id"),
                        row.getInt("position")
                ),
                playlistId
        );
    }

    public void ensureMovieExists(long tmdbId) {
        jdbc.update(
                """
                INSERT INTO movie (tmdb_id)
                VALUES (?)
                ON CONFLICT (tmdb_id) DO NOTHING
                """,
                tmdbId
        );
    }

    public void add(long playlistId, long tmdbId) {
        jdbc.update(
                """
                INSERT INTO playlist_movie (
                    playlist_id, tmdb_id, position
                )
                SELECT ?, ?, COALESCE(MAX(position), 0) + 1
                FROM playlist_movie
                WHERE playlist_id = ?
                ON CONFLICT (playlist_id, tmdb_id) DO NOTHING
                """,
                playlistId,
                tmdbId,
                playlistId
        );
    }

    public void remove(long playlistId, long tmdbId) {
        jdbc.update(
                """
                DELETE FROM playlist_movie
                WHERE playlist_id = ? AND tmdb_id = ?
                """,
                playlistId,
                tmdbId
        );
    }
}