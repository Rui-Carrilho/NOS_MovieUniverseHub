package com.movieuniverse.hub.playlist;
import com.movieuniverse.hub.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.List;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistManagementController {
    public record Rename(String name) {}
    public record Order(List<Long> movieIds) {}
    private final PlaylistManagementService service;
    private final CurrentUser current;
    public PlaylistManagementController(PlaylistManagementService service, CurrentUser current) {
        this.service = service; this.current = current;
    }
    @GetMapping("/deleted") public List<Playlist> deleted() { return service.deleted(current.required().id()); }
    @PatchMapping("/{id}") public ResponseEntity<Void> rename(@PathVariable("id") long id, @RequestBody Rename input) {
        service.rename(current.required().id(), id, input.name()); return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}/order") public ResponseEntity<Void> order(@PathVariable("id") long id, @RequestBody Order input) {
        service.reorder(current.required().id(), id, input.movieIds()); return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/export") public ResponseEntity<PlaylistManagementService.Export> export(@PathVariable("id") long id) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=playlist-" + id + ".json")
                .body(service.export(current.required().id(), id));
    }
}
