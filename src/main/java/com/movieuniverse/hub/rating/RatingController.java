package com.movieuniverse.hub.rating;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RatingController {
    public record RatingRequest(java.math.BigDecimal stars) {}
    private final RatingService ratings;
    private final ScoreService scores;
    public RatingController(RatingService ratings, ScoreService scores) {
        this.ratings = ratings;
        this.scores = scores;
    }

    @GetMapping("/movie-scores/{tmdbId}")
    public ScoreService.MovieScore score(@PathVariable("tmdbId") long tmdbId,
            @RequestParam(value = "userId", required = false) Long userId) {
        return scores.score(tmdbId, userId);
    }

    @PutMapping("/ratings/{tmdbId}")
    public ResponseEntity<Void> save(@PathVariable("tmdbId") long tmdbId,
            @RequestParam("userId") long userId, @RequestBody RatingRequest request) {
        Integer stars = null;
        if (request.stars() != null) {
            try {
                stars = request.stars().intValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("A classificação deve ser um inteiro de 1 a 10.");
            }
        }
        ratings.save(userId, tmdbId, stars);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/ratings/{tmdbId}")
    public ResponseEntity<Void> remove(@PathVariable("tmdbId") long tmdbId,
            @RequestParam("userId") long userId) {
        ratings.remove(userId, tmdbId);
        return ResponseEntity.noContent().build();
    }
}
