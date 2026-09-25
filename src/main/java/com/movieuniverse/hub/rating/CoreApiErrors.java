package com.movieuniverse.hub.rating;

import com.movieuniverse.hub.comparison.ComparisonController;
import com.movieuniverse.hub.movie.MovieApiException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {RatingController.class, ComparisonController.class})
public class CoreApiErrors {
    public record Error(String code, String message) {}

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Error> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new Error("INVALID_REQUEST", e.getMessage()));
    }
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Error> missing(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("NOT_FOUND", e.getMessage()));
    }
    @ExceptionHandler(MovieApiException.class)
    public ResponseEntity<Error> movie(MovieApiException e) {
        var response = ResponseEntity.status(e.status());
        if (e.retryAfter() != null) response.header(HttpHeaders.RETRY_AFTER, e.retryAfter().toString());
        return response.body(new Error(e.code(), e.getMessage()));
    }
}
