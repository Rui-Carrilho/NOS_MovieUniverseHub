package com.movieuniverse.hub.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<AppUser> rowMapper = (row, rowNumber) ->
            new AppUser(
                    row.getLong("id"),
                    row.getString("username")
            );

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AppUser> findAll() {
        return jdbc.query(
                "SELECT id, username FROM app_user ORDER BY username",
                rowMapper
        );
    }

    public Optional<AppUser> findById(long id) {
        return jdbc.query(
                "SELECT id, username FROM app_user WHERE id = ?",
                rowMapper,
                id
        ).stream().findFirst();
    }

    public Optional<AppUser> create(String username) {
        return jdbc.query(
                """
                INSERT INTO app_user (username)
                VALUES (?)
                ON CONFLICT (username) DO NOTHING
                RETURNING id, username
                """,
                rowMapper,
                username
        ).stream().findFirst();
    }
}