package com.movieuniverse.hub.rating;

import com.movieuniverse.hub.movie.MovieService;
import org.springframework.stereotype.Service;

@Service
public class ScoreService {
    public record MovieScore(long tmdbId, Double combinedRating, long totalVotes,
            Double localAverage, long localVotes, Integer userRating, String explanation) {}

    private final MovieService movies;
    private final RatingRepository ratings;
    private final RatingService users;
    private final CombinedScoreCalculator calculator = new CombinedScoreCalculator();

    public ScoreService(MovieService movies, RatingRepository ratings, RatingService users) {
        this.movies = movies;
        this.ratings = ratings;
        this.users = users;
    }

    public MovieScore score(long tmdbId, Long userId) {
        if (userId != null) users.requireUser(userId);
        var movie = movies.details(tmdbId);
        var local = ratings.totals(tmdbId);
        var score = calculator.calculate(movie.tmdbRating(), movie.tmdbVoteCount(), local.sum(), local.count());
        Integer own = userId == null ? null : ratings.find(userId, tmdbId).orElse(null);
        return new MovieScore(tmdbId, score.value(), score.actualVotes(),
                local.count() == 0 ? null : (double) local.sum() / local.count(),
                local.count(), own, score.explanation());
    }
}
