package com.movieuniverse.hub.playlist;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PlaylistManagementService {
    public record Export(Playlist playlist, List<MovieMembership> movies) {}
    private final JdbcTemplate jdbc;
    private final PlaylistMovieRepository memberships;
    public PlaylistManagementService(JdbcTemplate jdbc, PlaylistMovieRepository memberships) {
        this.jdbc = jdbc; this.memberships = memberships;
    }
    public List<Playlist> deleted(long userId) {
        return jdbc.query("SELECT id, name, user_id, deleted FROM playlist WHERE user_id = ? AND deleted = TRUE ORDER BY id",
                (row, n) -> new Playlist(row.getLong("id"), row.getString("name"), row.getLong("user_id"), row.getBoolean("deleted")), userId);
    }
    public void rename(long userId, long playlistId, String name) {
        if (name == null || name.isBlank() || name.strip().length() > 100)
            throw new IllegalArgumentException("O nome deve ter entre 1 e 100 caracteres.");
        if (jdbc.update("UPDATE playlist SET name = ? WHERE id = ? AND user_id = ? AND deleted = FALSE",
                name.strip(), playlistId, userId) != 1) throw new NoSuchElementException("Playlist não encontrada.");
    }
    @Transactional
    public void reorder(long userId, long playlistId, List<Long> ids) {
        lockOwned(userId, playlistId);
        var old = memberships.findAll(playlistId).stream().map(MovieMembership::tmdbId).toList();
        if (ids == null || ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size()
                || !new HashSet<>(ids).equals(new HashSet<>(old)))
            throw new IllegalArgumentException("A ordem deve conter exatamente os filmes atuais, sem duplicados.");
        // Replace within one transaction to avoid collisions in UNIQUE(playlist_id, position).
        jdbc.update("DELETE FROM playlist_movie WHERE playlist_id = ?", playlistId);
        for (int i = 0; i < ids.size(); i++)
            jdbc.update("INSERT INTO playlist_movie(playlist_id, tmdb_id, position) VALUES (?, ?, ?)",
                    playlistId, ids.get(i), i + 1);
    }
    @Transactional
    public Export export(long userId, long playlistId) {
        lockOwned(userId, playlistId);
        var playlist = jdbc.queryForObject("SELECT id, name, user_id, deleted FROM playlist WHERE id = ?",
                (row, n) -> new Playlist(row.getLong("id"), row.getString("name"), row.getLong("user_id"), row.getBoolean("deleted")), playlistId);
        return new Export(playlist, memberships.findAll(playlistId));
    }
    private void lockOwned(long userId, long playlistId) {
        if (jdbc.query("SELECT id FROM playlist WHERE id = ? AND user_id = ? AND deleted = FALSE FOR UPDATE",
                (row, n) -> row.getLong("id"), playlistId, userId).isEmpty())
            throw new NoSuchElementException("Playlist não encontrada.");
    }
}
