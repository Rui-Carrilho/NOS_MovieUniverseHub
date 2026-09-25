package com.movieuniverse.hub.movie;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
    private final MovieService service;
    public MovieController(MovieService service) { this.service = service; }

    @GetMapping("/search")
    public MovieSearchResponse search(@RequestParam("query") String query,
            @RequestParam(value = "page", defaultValue = "1") int page) {
        return service.search(query, page);
    }

    @GetMapping("/{tmdbId}")
    public MovieDetails details(@PathVariable("tmdbId") long tmdbId) {
        return service.details(tmdbId);
    }

    @ExceptionHandler(MovieApiException.class)
    public ResponseEntity<ApiError> unavailable(MovieApiException error) {
        var response = ResponseEntity.status(error.status());
        if (error.retryAfter() != null) response.header(HttpHeaders.RETRY_AFTER, error.retryAfter().toString());
        return response.body(new ApiError(error.code(), error.getMessage()));
    }

    public record ApiError(String code, String message) {}
}
