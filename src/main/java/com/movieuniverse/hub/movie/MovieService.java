package com.movieuniverse.hub.movie;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.movieuniverse.hub.tmdb.TmdbClient;
import com.movieuniverse.hub.tmdb.TmdbResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MovieService {
    private record SearchKey(String query, int page) {}
    private final TmdbClient tmdb;
    // Language is fixed for this service instance. Restarting clears its caches.
    private final Cache<SearchKey, MovieSearchResponse> searches = Caffeine.newBuilder()
            .maximumSize(200).expireAfterWrite(Duration.ofMinutes(5)).build();
    private final Cache<Long, MovieDetails> details = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(Duration.ofHours(1)).build();
    private final Cache<String, TmdbResponse.Images> configuration = Caffeine.newBuilder()
            .maximumSize(1).expireAfterWrite(Duration.ofHours(24)).build();

    public MovieService(TmdbClient tmdb) { this.tmdb = tmdb; }

    public MovieSearchResponse search(String query, int page) {
        if (query == null || query.isBlank() || query.strip().length() > 200) {
            throw invalid("A pesquisa deve ter entre 1 e 200 caracteres.");
        }
        if (page < 1 || page > 500) throw invalid("A página deve estar entre 1 e 500.");
        return searches.get(new SearchKey(query.strip(), page), key -> {
            var result = tmdb.search(key.query(), key.page());
            if (result.page() == null || result.page() != key.page() || result.totalPages() == null
                    || result.totalPages() < 0 || result.totalResults() == null || result.totalResults() < 0
                    || result.results() == null) throw MovieApiException.invalidResponse();
            var movies = result.results().stream().map(this::summary).toList();
            return new MovieSearchResponse(result.page(), Math.min(result.totalPages(), 500),
                    result.totalResults(), movies);
        });
    }

    public MovieDetails details(long id) {
        if (id <= 0) throw invalid("O identificador do filme deve ser positivo.");
        return details.get(id, movieId -> {
            var movie = tmdb.details(movieId);
            validate(movie);
            if (movie.id().longValue() != movieId.longValue()) throw MovieApiException.invalidResponse();
            var genres = movie.genres() == null ? List.<String>of() : movie.genres().stream()
                    .filter(g -> g != null && g.name() != null && !g.name().isBlank())
                    .map(TmdbResponse.Genre::name).toList();
            return new MovieDetails(movie.id(), movie.title(), date(movie.releaseDate()),
                    movie.overview(), genres,
                    movie.runtime() != null && movie.runtime() > 0 ? movie.runtime() : null,
                    poster(movie.posterPath()), rating(movie), movie.voteCount());
        });
    }

    private MovieSummary summary(TmdbResponse.Movie movie) {
        validate(movie);
        return new MovieSummary(movie.id(), movie.title(), date(movie.releaseDate()),
                poster(movie.posterPath()), rating(movie), movie.voteCount());
    }

    private void validate(TmdbResponse.Movie movie) {
        if (movie == null || movie.id() == null || movie.id() <= 0 || movie.title() == null
                || movie.title().isBlank() || movie.voteCount() == null || movie.voteCount() < 0) {
            throw MovieApiException.invalidResponse();
        }
        if (movie.voteCount() > 0 && (movie.voteAverage() == null || !Double.isFinite(movie.voteAverage())
                || movie.voteAverage() < 0 || movie.voteAverage() > 10)) throw MovieApiException.invalidResponse();
    }

    private Double rating(TmdbResponse.Movie movie) { return movie.voteCount() == 0 ? null : movie.voteAverage(); }
    private String date(String date) { return date == null || date.isBlank() ? null : date; }

    private String poster(String path) {
        if (path == null || path.isBlank()) return null;
        if (!path.matches("/[A-Za-z0-9._-]+")) return null;
        var images = configuration.get("images", key -> {
            var value = tmdb.configuration().images();
            // Only permit TMDB's HTTPS image host, never arbitrary upstream URLs.
            if (value == null || !"https://image.tmdb.org/t/p/".equals(value.secureBaseUrl())
                    || value.posterSizes() == null) throw MovieApiException.invalidResponse();
            return value;
        });
        String size = images.posterSizes().contains("w500") ? "w500"
                : images.posterSizes().contains("original") ? "original" : null;
        return size == null ? null : images.secureBaseUrl() + size + path;
    }

    private MovieApiException invalid(String message) {
        return new MovieApiException(HttpStatus.BAD_REQUEST, "INVALID_MOVIE_REQUEST", message);
    }
}
