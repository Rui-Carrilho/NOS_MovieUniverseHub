package com.movieuniverse.hub.playlist;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/playlists/{playlistId}/movies")
public class PlaylistMovieController {

    private final PlaylistMovieService service;

    public PlaylistMovieController(PlaylistMovieService service) {
        this.service = service;
    }

    @GetMapping
    public List<MovieMembership> findAll(
            @PathVariable("playlistId") long playlistId
    ) {
        return service.findAll(playlistId);
    }

    @PutMapping("/{tmdbId}")
    public ResponseEntity<Void> add(
            @PathVariable("playlistId") long playlistId,
            @PathVariable("tmdbId") long tmdbId
    ) {
        service.add(playlistId, tmdbId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tmdbId}")
    public ResponseEntity<Void> remove(
            @PathVariable("playlistId") long playlistId,
            @PathVariable("tmdbId") long tmdbId
    ) {
        service.remove(playlistId, tmdbId);

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleMissingPlaylist(
            NoSuchElementException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleInvalidInput(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest()
                .body(new ApiError(exception.getMessage()));
    }

    public record ApiError(String message) {
    }
}