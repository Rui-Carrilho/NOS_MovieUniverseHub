package com.movieuniverse.hub.movie;

import org.springframework.http.HttpStatus;

/** Contains safe public messages only, never upstream bodies or credentials. */
public class MovieApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Integer retryAfter;

    public MovieApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public MovieApiException(HttpStatus status, String code, String message, Integer retryAfter) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfter = retryAfter;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public Integer retryAfter() { return retryAfter; }

    public static MovieApiException invalidResponse() {
        return new MovieApiException(HttpStatus.BAD_GATEWAY, "INVALID_MOVIE_RESPONSE",
                "O serviço de filmes devolveu informação inválida. Tenta novamente.");
    }
}
