package com.movieuniverse.hub.playlist;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlaylistRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Playlist> rowMapper = (row, rowNumber) ->
            new Playlist(
                    row.getLong("id"),
                    row.getString("name"),
                    row.getLong("user_id"),
                    row.getBoolean("deleted")
            );

    public PlaylistRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Playlist> findAll(long userId) {
        return jdbc.query(
                """
                SELECT id, name, user_id, deleted
                FROM playlist
                WHERE user_id = ? AND deleted = FALSE
                ORDER BY id
                """,
                rowMapper,
                userId
        );
    }

    public Optional<Playlist> findById(long id) {
        return jdbc.query(
                """
                SELECT id, name, user_id, deleted
                FROM playlist
                WHERE id = ? AND deleted = FALSE
                """,
                rowMapper,
                id
        ).stream().findFirst();
    }

    public Playlist create(String name, Long userId) {
        return jdbc.queryForObject(
                """
                INSERT INTO playlist (name, user_id)
                VALUES (?, ?)
                RETURNING id, name, user_id, deleted
                """,
                rowMapper,
                name,
                userId
        );
    }

    public int setDeleted(long id, long userId, boolean deleted) {
        return jdbc.update(
                """
                UPDATE playlist
                SET deleted = ?
                WHERE id = ? AND user_id = ?
                """,
                deleted,
                id,
                userId
        );
    }
}