package com.movieuniverse.hub.playlist;

public record Playlist(
        long id,
        String name,
        long userId,
        boolean deleted
) {
}