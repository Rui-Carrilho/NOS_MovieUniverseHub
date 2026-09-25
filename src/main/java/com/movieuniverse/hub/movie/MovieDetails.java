package com.movieuniverse.hub.movie;

import java.util.List;

public record MovieDetails(long tmdbId, String title, String releaseDate,
        String overview, List<String> genres, Integer runtimeMinutes,
        String posterUrl, Double tmdbRating, long tmdbVoteCount) {}
