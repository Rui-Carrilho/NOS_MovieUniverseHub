package com.movieuniverse.hub.batch1;

import com.movieuniverse.hub.movie.*;
import com.movieuniverse.hub.tmdb.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovieServiceTest {
    private final TmdbClient client = mock(TmdbClient.class);
    private final MovieService service = new MovieService(client);

    private TmdbResponse.Movie movie(long id, long votes, Double rating) {
        return new TmdbResponse.Movie(id, "A movie", "Story", "2020-01-01", null, rating, votes, 0, null);
    }
    @Test void comparesBoxedMovieIdsByValueAndCachesDetails() {
        when(client.details(27205L)).thenReturn(movie(27205L, 100, 8.0));
        assertThat(service.details(27205).tmdbId()).isEqualTo(27205);
        assertThat(service.details(27205).runtimeMinutes()).isNull();
        verify(client, times(1)).details(27205);
    }
    @Test void noVotesIsNullAndSearchTrimsItsCacheKey() {
        when(client.search("Alien", 1)).thenReturn(new TmdbResponse.Search(1, 1, 1L, List.of(movie(1, 0, 0.0))));
        assertThat(service.search(" Alien ", 1).results().getFirst().tmdbRating()).isNull();
        service.search("Alien", 1);
        verify(client, times(1)).search("Alien", 1);
        verify(client, never()).configuration();
    }
    @Test void invalidParametersNeverReachTmdb() {
        assertThatThrownBy(() -> service.search(" ", 1)).isInstanceOf(MovieApiException.class);
        assertThatThrownBy(() -> service.search("Alien", 0)).isInstanceOf(MovieApiException.class);
        assertThatThrownBy(() -> service.details(-1)).isInstanceOf(MovieApiException.class);
        verifyNoInteractions(client);
    }
    @Test void failedLoadsAreNotCached() {
        when(client.details(1)).thenThrow(MovieApiException.invalidResponse()).thenReturn(movie(1, 100, 8.0));
        assertThatThrownBy(() -> service.details(1)).isInstanceOf(MovieApiException.class);
        assertThat(service.details(1).tmdbRating()).isEqualTo(8.0);
        verify(client, times(2)).details(1);
    }
    @Test void posterConfigurationIsSharedAndDetailsRetainRuntimeAndGenres() {
        var dto = new TmdbResponse.Movie(1L, "A", "Plot", "2020-01-01", "/poster.jpg", 8.0, 2L, 120,
                List.of(new TmdbResponse.Genre(1L, "Drama")));
        when(client.details(1)).thenReturn(dto);
        when(client.configuration()).thenReturn(new TmdbResponse.Configuration(
                new TmdbResponse.Images("https://image.tmdb.org/t/p/", List.of("w500"))));
        assertThat(service.details(1).posterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
        assertThat(service.details(1).genres()).containsExactly("Drama");
        assertThat(service.details(1).runtimeMinutes()).isEqualTo(120);
        verify(client, times(1)).configuration();
    }
}
