package com.movieuniverse.hub.movie;

import java.util.List;

public record MovieSearchResponse(int page, int totalPages, long totalResults,
        List<MovieSummary> results) {}
