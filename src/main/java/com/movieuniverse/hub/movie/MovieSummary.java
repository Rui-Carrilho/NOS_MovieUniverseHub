package com.movieuniverse.hub.movie;

public record MovieSummary(long tmdbId, String title, String releaseDate,
        String posterUrl, Double tmdbRating, long tmdbVoteCount) {}
