package com.movieuniverse.hub.playlist;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PlaylistMovieService {

    private final PlaylistRepository playlists;
    private final PlaylistMovieRepository memberships;

    public PlaylistMovieService(
            PlaylistRepository playlists,
            PlaylistMovieRepository memberships
    ) {
        this.playlists = playlists;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public List<MovieMembership> findAll(long playlistId) {
        playlists.findById(playlistId)
                .orElseThrow(() ->
                        new NoSuchElementException("Playlist not found")
                );

        return memberships.findAll(playlistId);
    }

    @Transactional
    public void add(long playlistId, long tmdbId) {
        validateMovieId(tmdbId);
        requireAndLockPlaylist(playlistId);

        memberships.ensureMovieExists(tmdbId);
        memberships.add(playlistId, tmdbId);
    }

    @Transactional
    public void remove(long playlistId, long tmdbId) {
        validateMovieId(tmdbId);
        requireAndLockPlaylist(playlistId);

        memberships.remove(playlistId, tmdbId);
    }

    private void validateMovieId(long tmdbId) {
        if (tmdbId <= 0) {
            throw new IllegalArgumentException(
                    "TMDB ID must be positive"
            );
        }
    }

    private void requireAndLockPlaylist(long playlistId) {
        if (!memberships.lockPlaylist(playlistId)) {
            throw new NoSuchElementException("Playlist not found");
        }
    }
}