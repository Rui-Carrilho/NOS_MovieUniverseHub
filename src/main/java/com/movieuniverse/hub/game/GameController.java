package com.movieuniverse.hub.game;
import com.movieuniverse.hub.auth.CurrentUser;
import com.movieuniverse.hub.movie.MovieApiException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;

@RestController
@RequestMapping("/api/game")
public class GameController {
    public record Answer(String roundId, Long tmdbId) {}
    private final GameService game;
    private final CurrentUser current;
    public GameController(GameService game, CurrentUser current) { this.game = game; this.current = current; }
    @PostMapping("/start") public GameService.View start() { return game.start(current.required().id()); }
    @PostMapping("/answer") public GameService.View answer(@RequestBody Answer answer) {
        return game.answer(current.required().id(), answer.roundId(), answer.tmdbId());
    }
    @GetMapping("/highscores") public List<GameService.Highscore> highscores() { return game.highscores(); }
    @ExceptionHandler(MovieApiException.class)
    public ResponseEntity<Map<String,String>> unavailable(MovieApiException exception) {
        var response = ResponseEntity.status(exception.status());
        if (exception.retryAfter() != null) response.header("Retry-After", exception.retryAfter().toString());
        return response.body(Map.of("message", exception.getMessage()));
    }
}
