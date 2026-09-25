package com.movieuniverse.hub.rating;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RatingRepository {
    public record LocalVotes(long count, long sum) {}
    private final JdbcTemplate jdbc;
    public RatingRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public LocalVotes totals(long tmdbId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS vote_count, COALESCE(SUM(stars), 0) AS vote_sum
                FROM user_rating WHERE tmdb_id = ?
                """, (row, number) -> new LocalVotes(row.getLong("vote_count"), row.getLong("vote_sum")), tmdbId);
    }

    public Optional<Integer> find(long userId, long tmdbId) {
        return jdbc.query("SELECT stars FROM user_rating WHERE user_id = ? AND tmdb_id = ?",
                (row, number) -> row.getInt("stars"), userId, tmdbId).stream().findFirst();
    }

    public void save(long userId, long tmdbId, int stars) {
        jdbc.update("INSERT INTO movie (tmdb_id) VALUES (?) ON CONFLICT (tmdb_id) DO NOTHING", tmdbId);
        jdbc.update("""
                INSERT INTO user_rating (user_id, tmdb_id, stars, rated_on)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, tmdb_id)
                DO UPDATE SET stars = EXCLUDED.stars, rated_on = EXCLUDED.rated_on
                """, userId, tmdbId, stars, LocalDate.now());
    }

    public void remove(long userId, long tmdbId) {
        jdbc.update("DELETE FROM user_rating WHERE user_id = ? AND tmdb_id = ?", userId, tmdbId);
    }
}
