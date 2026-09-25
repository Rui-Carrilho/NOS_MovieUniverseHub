package com.movieuniverse.hub.batch1;

import com.movieuniverse.hub.movie.MovieApiException;
import com.movieuniverse.hub.tmdb.TmdbClient;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TmdbClientTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test/3")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer fixture-token");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final TmdbClient client = new TmdbClient(builder.build(), "fixture-token", "pt-PT");

    @Test void encodesQueryAndKeepsAuthenticationInHeader() {
        server.expect(requestTo("https://example.test/3/search/movie?query=Fast%20%26%20Furious&page=1&language=pt-PT&include_adult=false"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fixture-token"))
                .andRespond(withSuccess("""
                        {"page":1,"total_pages":0,"total_results":0,"results":[]}
                        """, MediaType.APPLICATION_JSON));
        assertThat(client.search("Fast & Furious", 1).results()).isEmpty();
        server.verify();
    }
    @Test void missingTokenMakesNoRequest() {
        var missing = new TmdbClient(builder.build(), "", "pt-PT");
        assertThatThrownBy(() -> missing.details(1)).isInstanceOfSatisfying(MovieApiException.class,
                e -> assertThat(e.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        server.verify();
    }
    @Test void upstreamAuthBodyDoesNotLeak() {
        server.expect(requestTo("https://example.test/3/movie/1?language=pt-PT"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("sensitive-upstream-error"));
        assertThatThrownBy(() -> client.details(1)).isInstanceOfSatisfying(MovieApiException.class, e -> {
            assertThat(e.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(e.getMessage()).doesNotContain("sensitive", "fixture-token");
            assertThat(e.getCause()).isNull();
        });
        server.verify();
    }
    @Test void missingMovieBecomes404AndRateLimitBecomesRetryable503() {
        server.expect(requestTo("https://example.test/3/movie/1?language=pt-PT")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("https://example.test/3/movie/2?language=pt-PT")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> client.details(1)).isInstanceOfSatisfying(MovieApiException.class,
                e -> assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> client.details(2)).isInstanceOfSatisfying(MovieApiException.class, e -> {
            assertThat(e.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(e.retryAfter()).isEqualTo(30);
        });
        server.verify();
    }
    @Test void timeoutBecomes504() {
        server.expect(requestTo("https://example.test/3/movie/1?language=pt-PT"))
                .andRespond(withException(new SocketTimeoutException("fixture timeout")));
        assertThatThrownBy(() -> client.details(1)).isInstanceOfSatisfying(MovieApiException.class,
                e -> assertThat(e.status()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }
    @Test void malformedJsonBecomes502() {
        server.expect(requestTo("https://example.test/3/movie/1?language=pt-PT"))
                .andRespond(withSuccess("{not JSON", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.details(1)).isInstanceOfSatisfying(MovieApiException.class,
                e -> assertThat(e.status()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
