package com.movieuniverse.hub.playlist;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService service;
    private final com.movieuniverse.hub.auth.CurrentUser current;

    public PlaylistController(PlaylistService service, com.movieuniverse.hub.auth.CurrentUser current) {
        this.current = current;
        this.service = service;
    }

    @GetMapping
    public List<Playlist> findAll(
            @RequestParam("userId") long userId
    ) {
        return service.findAll(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Playlist> findById(
            @PathVariable("id") long id
    ) {
        return ResponseEntity.of(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Playlist> create(
            @RequestBody CreatePlaylistRequest request
    ) {
        current.require(request.userId());
        Playlist playlist = service.create(
                request.name(),
                request.userId()
        );

        URI location = URI.create("/api/playlists/" + playlist.id());

        return ResponseEntity.created(location).body(playlist);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") long id,
            @RequestParam("userId") long userId
    ) {
        service.setDeleted(id, userId, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(
            @PathVariable("id") long id,
            @RequestParam("userId") long userId
    ) {
        service.setDeleted(id, userId, false);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleMissingResource(
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