package com.movieuniverse.hub.tmdb;

import com.movieuniverse.hub.movie.MovieApiException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TmdbClient {
    private final RestClient client;
    private final String token;
    private final String language;

    public TmdbClient(@Qualifier("tmdbRestClient") RestClient client,
            @Value("${tmdb.api-token:}") String token,
            @Value("${tmdb.language:pt-PT}") String language) {
        this.client = client;
        this.token = token;
        this.language = language;
    }

    public TmdbResponse.Search search(String query, int page) {
        String path = UriComponentsBuilder.fromPath("/search/movie")
                .queryParam("query", "{query}").queryParam("page", page)
                .queryParam("language", "{language}").queryParam("include_adult", false)
                .encode().buildAndExpand(query, language).toUriString();
        return get(path, TmdbResponse.Search.class, false);
    }

    public TmdbResponse.Movie details(long id) {
        String path = UriComponentsBuilder.fromPath("/movie/" + id)
                .queryParam("language", "{language}").encode().buildAndExpand(language).toUriString();
        return get(path, TmdbResponse.Movie.class, true);
    }

    public TmdbResponse.Configuration configuration() {
        return get("/configuration", TmdbResponse.Configuration.class, false);
    }

    private <T> T get(String path, Class<T> type, boolean movieLookup) {
        if (token.isBlank()) {
            throw new MovieApiException(HttpStatus.SERVICE_UNAVAILABLE, "TMDB_NOT_CONFIGURED",
                    "Configura TMDB_API_TOKEN no ficheiro .env e reinicia a aplicação.");
        }
        try {
            // The relative URI is already encoded. Resolve it against the client's base URL.
            T result = client.get().uri(builder -> URI.create(builder.build().toString() + path))
                    .retrieve().body(type);
            if (result == null) throw MovieApiException.invalidResponse();
            return result;
        } catch (RestClientResponseException error) {
            int status = error.getStatusCode().value();
            if (status == 404 && movieLookup) {
                throw new MovieApiException(HttpStatus.NOT_FOUND, "MOVIE_NOT_FOUND", "Filme não encontrado.");
            }
            if (status == 401 || status == 403) {
                throw new MovieApiException(HttpStatus.SERVICE_UNAVAILABLE, "TMDB_CONFIGURATION_ERROR",
                        "O acesso ao serviço de filmes não está configurado corretamente.");
            }
            if (status == 429) {
                throw new MovieApiException(HttpStatus.SERVICE_UNAVAILABLE, "TMDB_RATE_LIMITED",
                        "O serviço de filmes está ocupado. Tenta novamente dentro de instantes.", 30);
            }
            throw new MovieApiException(HttpStatus.BAD_GATEWAY, "TMDB_UNAVAILABLE",
                    "O serviço de filmes está temporariamente indisponível.");
        } catch (ResourceAccessException error) {
            boolean timeout = false;
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) timeout = true;
            }
            throw new MovieApiException(timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                    timeout ? "TMDB_TIMEOUT" : "TMDB_UNREACHABLE",
                    "Não foi possível contactar o serviço de filmes. Tenta novamente.");
        } catch (RestClientException error) {
            throw MovieApiException.invalidResponse();
        }
    }
}
