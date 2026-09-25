package com.movieuniverse.hub.game;

import com.github.benmanes.caffeine.cache.*;
import com.movieuniverse.hub.movie.*;
import com.movieuniverse.hub.rating.ScoreService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.*;

@Service
public class GameService {
    public static final double MIN_DIFFERENCE = 0.5;
    public static final long MIN_VOTES = 100;
    public record Card(long tmdbId, String title, String releaseDate, String posterUrl) {}
    public record Reveal(boolean correct, double leftScore, double rightScore) {}
    public record View(String roundId, int score, boolean finished, String reason, Card left, Card right, Reveal previous) {}
    public record Highscore(String username, int score) {}
    record Candidate(Card card, double value) {}
    record Pair(Candidate left, Candidate right) {}
    static final class Game {
        final List<Pair> remaining;
        Pair pair;
        String roundId;
        int score;
        boolean finished;
        Game(List<Pair> remaining) { this.remaining = remaining; advance(); }
        void advance() {
            if (remaining.isEmpty()) { finished = true; pair = null; roundId = null; return; }
            pair = remaining.removeLast();
            roundId = UUID.randomUUID().toString();
        }
    }
    private final JdbcTemplate jdbc;
    private final ScoreService scores;
    private final MovieService movies;
    private final Cache<Long, Game> games = Caffeine.newBuilder().maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(30)).build();
    public GameService(JdbcTemplate jdbc, ScoreService scores, MovieService movies) {
        this.jdbc = jdbc; this.scores = scores; this.movies = movies;
    }
    public View start(long userId) {
        var ids = jdbc.query("""
                SELECT DISTINCT pm.tmdb_id FROM playlist_movie pm
                JOIN playlist p ON p.id = pm.playlist_id
                WHERE p.user_id = ? AND p.deleted = FALSE ORDER BY pm.tmdb_id LIMIT 100
                """, (row, n) -> row.getLong("tmdb_id"), userId);
        var candidates = new ArrayList<Candidate>();
        for (long id : ids) {
            try {
                var score = scores.score(id, null); // Same central scoring function as details and comparisons.
                if (score.combinedRating() == null || score.totalVotes() < MIN_VOTES) continue;
                var movie = movies.details(id);
                candidates.add(new Candidate(new Card(id, movie.title(), movie.releaseDate(), movie.posterUrl()), score.combinedRating()));
            } catch (MovieApiException exception) {
                if (exception.status().is5xxServerError()) throw exception;
                // A removed TMDB movie is ineligible, not a zero-rated movie.
            }
        }
        var pairs = new ArrayList<Pair>();
        for (int i = 0; i < candidates.size(); i++)
            for (int k = i + 1; k < candidates.size(); k++) {
                var a = candidates.get(i); var b = candidates.get(k);
                if (Math.abs(a.value() - b.value()) >= MIN_DIFFERENCE)
                    pairs.add(java.util.concurrent.ThreadLocalRandom.current().nextBoolean() ? new Pair(a, b) : new Pair(b, a));
            }
        if (pairs.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Adiciona às tuas playlists filmes com pelo menos 100 votos e notas combinadas separadas por 0,5 pontos.");
        Collections.shuffle(pairs);
        var game = new Game(pairs);
        games.put(userId, game);
        return view(game, "PLAYING", null);
    }
    public View answer(long userId, String roundId, Long tmdbId) {
        var game = games.getIfPresent(userId);
        if (game == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Inicia um novo jogo.");
        synchronized (game) {
            if (games.getIfPresent(userId) != game || game.finished || roundId == null || !roundId.equals(game.roundId))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta ronda já terminou ou expirou.");
            var pair = game.pair;
            if (tmdbId == null || (tmdbId != pair.left().card().tmdbId() && tmdbId != pair.right().card().tmdbId()))
                throw new IllegalArgumentException("Escolhe um dos dois filmes.");
            long winner = pair.left().value() > pair.right().value() ? pair.left().card().tmdbId() : pair.right().card().tmdbId();
            boolean correct = tmdbId == winner;
            var reveal = new Reveal(correct, pair.left().value(), pair.right().value());
            if (!correct) {
                game.finished = true; game.roundId = null; game.pair = null;
                return view(game, "WRONG_ANSWER", reveal);
            }
            // Persist before advancing: failed writes can be retried with the same round token.
            int nextScore = game.score + 1;
            jdbc.update("""
                    INSERT INTO game_highscore(user_id, score) VALUES (?, ?)
                    ON CONFLICT(user_id) DO UPDATE SET score = EXCLUDED.score, achieved_at = CURRENT_TIMESTAMP
                    WHERE game_highscore.score < EXCLUDED.score
                    """, userId, nextScore);
            game.score = nextScore;
            game.advance();
            return view(game, game.finished ? "ALL_PAIRS_COMPLETED" : "PLAYING", reveal);
        }
    }
    public List<Highscore> highscores() {
        return jdbc.query("""
                SELECT u.username, h.score FROM game_highscore h JOIN app_user u ON u.id = h.user_id
                ORDER BY h.score DESC, h.achieved_at, u.id LIMIT 20
                """, (row, n) -> new Highscore(row.getString("username"), row.getInt("score")));
    }
    private View view(Game game, String reason, Reveal reveal) {
        return new View(game.roundId, game.score, game.finished, reason,
                game.pair == null ? null : game.pair.left().card(),
                game.pair == null ? null : game.pair.right().card(), reveal);
    }
}
