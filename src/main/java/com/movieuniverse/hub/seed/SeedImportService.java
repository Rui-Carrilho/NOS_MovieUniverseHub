package com.movieuniverse.hub.seed;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

@Service
public class SeedImportService {

    public record ImportReport(
            boolean alreadyImported,
            int usersInserted,
            int playlistsInserted,
            int moviesInserted,
            int membershipsInserted,
            int ratingsInserted,
            int duplicateMembershipsSkipped
    ) {
    }

    private final ObjectMapper objectMapper;
    private final SeedValidator validator;
    private final JdbcTemplate jdbc;

    public SeedImportService(
            ObjectMapper objectMapper,
            SeedValidator validator,
            JdbcTemplate jdbc
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.jdbc = jdbc;
    }

    @Transactional
    public ImportReport importBytes(byte[] bytes) {
        SeedData seed;

        try {
            seed = objectMapper.readValue(bytes, SeedData.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid seed JSON",
                    exception
            );
        }

        validator.validate(seed);
        String checksum = checksum(bytes);

        // The lock lasts until this transaction commits or rolls back.
        jdbc.execute("SELECT pg_advisory_xact_lock(71024001)");

        Boolean imported = jdbc.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM seed_import
                    WHERE checksum = ?
                )
                """,
                Boolean.class,
                checksum
        );

        if (Boolean.TRUE.equals(imported)) {
            return new ImportReport(
                    true, 0, 0, 0, 0, 0, 0
            );
        }

        int usersInserted = 0;
        int playlistsInserted = 0;
        int moviesInserted = 0;
        int membershipsInserted = 0;
        int ratingsInserted = 0;
        int duplicatesSkipped = 0;

        for (SeedData.User user : seed.utilizadores()) {
            usersInserted += jdbc.update(
                    """
                    INSERT INTO app_user (username)
                    VALUES (?)
                    ON CONFLICT (username) DO NOTHING
                    """,
                    user.nome()
            );
        }

        for (SeedData.Playlist playlist : seed.playlists()) {
            long ownerId = userId(playlist.utilizador());

            int inserted = jdbc.update(
                    """
                    INSERT INTO playlist (
                        source_id, name, user_id, deleted
                    )
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (source_id) DO NOTHING
                    """,
                    playlist.id(),
                    playlist.nome(),
                    ownerId,
                    playlist.apagada()
            );

            playlistsInserted += inserted;

            // Preserve edits to a playlist that was imported earlier.
            if (inserted == 0) {
                continue;
            }

            Long playlistId = jdbc.queryForObject(
                    """
                    SELECT id
                    FROM playlist
                    WHERE source_id = ?
                    """,
                    Long.class,
                    playlist.id()
            );

            Set<Long> seenMovieIds = new HashSet<>();

            for (SeedData.Movie movie : playlist.filmes()) {
                if (!seenMovieIds.add(movie.tmdb_id())) {
                    duplicatesSkipped++;
                    continue;
                }

                moviesInserted += ensureMovieExists(
                        movie.tmdb_id()
                );

                membershipsInserted += jdbc.update(
                        """
                        INSERT INTO playlist_movie (
                            playlist_id, tmdb_id, position
                        )
                        VALUES (?, ?, ?)
                        """,
                        playlistId,
                        movie.tmdb_id(),
                        movie.ordem()
                );
            }
        }

        for (SeedData.Rating rating : seed.notas()) {
            moviesInserted += ensureMovieExists(
                    rating.tmdb_id()
            );

            ratingsInserted += jdbc.update(
                    """
                    INSERT INTO user_rating (
                        user_id, tmdb_id, stars, rated_on
                    )
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (user_id, tmdb_id)
                    DO NOTHING
                    """,
                    userId(rating.utilizador()),
                    rating.tmdb_id(),
                    rating.estrelas(),
                    rating.data()
            );
        }

        // This entry commits with all the data above.
        jdbc.update(
                "INSERT INTO seed_import (checksum) VALUES (?)",
                checksum
        );

        return new ImportReport(
                false,
                usersInserted,
                playlistsInserted,
                moviesInserted,
                membershipsInserted,
                ratingsInserted,
                duplicatesSkipped
        );
    }

    private long userId(String username) {
        Long id = jdbc.queryForObject(
                """
                SELECT id
                FROM app_user
                WHERE username = ?
                """,
                Long.class,
                username
        );

        if (id == null) {
            throw new IllegalStateException(
                    "User was not found after validation"
            );
        }

        return id;
    }

    private int ensureMovieExists(long tmdbId) {
        return jdbc.update(
                """
                INSERT INTO movie (tmdb_id)
                VALUES (?)
                ON CONFLICT (tmdb_id) DO NOTHING
                """,
                tmdbId
        );
    }

    private String checksum(byte[] bytes) {
        try {
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(bytes);

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}