package com.movieuniverse.hub.comparison;

import com.movieuniverse.hub.movie.MovieApiException;
import com.movieuniverse.hub.playlist.Playlist;
import com.movieuniverse.hub.playlist.PlaylistMovieRepository;
import com.movieuniverse.hub.playlist.PlaylistRepository;
import com.movieuniverse.hub.rating.RatingService;
import com.movieuniverse.hub.rating.ScoreService;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ComparisonService {
    public record ListScore(long playlistId, String name, int movieCount, int ratedMovies,
            int unratedMovies, int unavailableMovies, Double average) {}
    public record Comparison(ListScore left, ListScore right, List<Long> commonMovies,
            List<Long> onlyLeft, List<Long> onlyRight, double overlapPercent,
            String outcome, Long winnerPlaylistId) {}

    private final PlaylistRepository playlists;
    private final PlaylistMovieRepository memberships;
    private final ScoreService scores;
    private final RatingService users;

    public ComparisonService(PlaylistRepository playlists, PlaylistMovieRepository memberships,
            ScoreService scores, RatingService users) {
        this.playlists = playlists;
        this.memberships = memberships;
        this.scores = scores;
        this.users = users;
    }

    public Comparison compare(long userId, long leftId, long rightId) {
        users.requireUser(userId);
        if (leftId == rightId) throw new IllegalArgumentException("Escolhe duas playlists diferentes.");
        Playlist left = owned(leftId, userId);
        Playlist right = owned(rightId, userId);
        List<Long> a = ids(leftId);
        List<Long> b = ids(rightId);
        Set<Long> union = new LinkedHashSet<>(a);
        union.addAll(b);
        // Each shared movie is evaluated once so both sides use the same snapshot.
        Map<Long, ScoreService.MovieScore> snapshot = new HashMap<>();
        Set<Long> unavailable = new HashSet<>();
        for (long id : union) {
            try { snapshot.put(id, scores.score(id, null)); }
            catch (MovieApiException error) {
                // Keep unavailable distinct from genuinely unrated; never award a partial-data winner.
                unavailable.add(id);
                if (error.status().is5xxServerError()) {
                    // A provider outage affects remaining lookups too; do not multiply retries/timeouts.
                    union.stream().filter(movieId -> !snapshot.containsKey(movieId)).forEach(unavailable::add);
                    break;
                }
            }
        }
        var leftScore = summarize(left, a, snapshot, unavailable);
        var rightScore = summarize(right, b, snapshot, unavailable);
        String outcome;
        Long winner = null;
        if (!unavailable.isEmpty()) outcome = "INCOMPLETE";
        else if (leftScore.average() == null || rightScore.average() == null) outcome = "INSUFFICIENT_DATA";
        else if (Math.abs(leftScore.average() - rightScore.average()) < 0.000000001) outcome = "TIE";
        else {
            outcome = "WINNER";
            winner = leftScore.average() > rightScore.average() ? leftId : rightId;
        }
        Set<Long> bSet = new HashSet<>(b);
        Set<Long> aSet = new HashSet<>(a);
        List<Long> common = a.stream().filter(bSet::contains).toList();
        return new Comparison(leftScore, rightScore, common,
                a.stream().filter(id -> !bSet.contains(id)).toList(),
                b.stream().filter(id -> !aSet.contains(id)).toList(),
                union.isEmpty() ? 0 : 100.0 * common.size() / union.size(), outcome, winner);
    }

    private Playlist owned(long id, long userId) {
        return playlists.findById(id).filter(p -> p.userId() == userId)
                .orElseThrow(() -> new NoSuchElementException("Playlist não encontrada neste perfil."));
    }

    private List<Long> ids(long playlistId) {
        return memberships.findAll(playlistId).stream().map(m -> m.tmdbId()).distinct().toList();
    }

    private ListScore summarize(Playlist playlist, List<Long> ids,
            Map<Long, ScoreService.MovieScore> snapshot, Set<Long> unavailable) {
        int missing = 0, unrated = 0, rated = 0;
        double sum = 0;
        for (long id : ids) {
            if (unavailable.contains(id)) { missing++; continue; }
            Double value = snapshot.get(id).combinedRating();
            if (value == null) unrated++;
            else { rated++; sum += value; }
        }
        return new ListScore(playlist.id(), playlist.name(), ids.size(), rated, unrated, missing,
                rated == 0 ? null : sum / rated);
    }
}
