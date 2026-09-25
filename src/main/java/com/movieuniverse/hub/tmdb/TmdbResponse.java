package com.movieuniverse.hub.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** External DTOs: TMDB can add fields without changing our public API. */
public final class TmdbResponse {
    private TmdbResponse() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Movie(Long id, String title, String overview,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("vote_average") Double voteAverage,
            @JsonProperty("vote_count") Long voteCount,
            Integer runtime, List<Genre> genres) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genre(Long id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Search(Integer page,
            @JsonProperty("total_pages") Integer totalPages,
            @JsonProperty("total_results") Long totalResults,
            List<Movie> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Configuration(Images images) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Images(@JsonProperty("secure_base_url") String secureBaseUrl,
            @JsonProperty("poster_sizes") List<String> posterSizes) {}
}
