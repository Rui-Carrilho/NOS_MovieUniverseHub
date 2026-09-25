package com.movieuniverse.hub.batch1;

import com.movieuniverse.hub.comparison.ComparisonService;
import com.movieuniverse.hub.movie.MovieApiException;
import com.movieuniverse.hub.playlist.*;
import com.movieuniverse.hub.rating.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComparisonServiceTest {
    private final PlaylistRepository playlists = mock(PlaylistRepository.class);
    private final PlaylistMovieRepository memberships = mock(PlaylistMovieRepository.class);
    private final ScoreService scores = mock(ScoreService.class);
    private final RatingService users = mock(RatingService.class);
    private final ComparisonService service = new ComparisonService(playlists, memberships, scores, users);

    private void playlist(long id, long... movies) {
        when(playlists.findById(id)).thenReturn(Optional.of(new Playlist(id, "List " + id, 1, false)));
        when(memberships.findAll(id)).thenReturn(Arrays.stream(movies).mapToObj(m -> new MovieMembership(m, 1)).toList());
    }
    private void score(long id, Double value) {
        when(scores.score(id, null)).thenReturn(new ScoreService.MovieScore(id, value, 10, null, 0, null, "Explanation"));
    }
    @Test void averagesMoviesEquallyAndComputesOverlapFromDistinctIds() {
        playlist(1, 10, 20, 20); playlist(2, 20, 30);
        score(10, 6.0); score(20, 8.0); score(30, 10.0);
        var result = service.compare(1, 1, 2);
        assertThat(result.left().average()).isEqualTo(7.0);
        assertThat(result.right().average()).isEqualTo(9.0);
        assertThat(result.winnerPlaylistId()).isEqualTo(2);
        assertThat(result.commonMovies()).containsExactly(20L);
        assertThat(result.overlapPercent()).isCloseTo(100.0 / 3, within(0.000000001));
        verify(scores, times(1)).score(20, null);
    }
    @Test void unratedMoviesDoNotBecomeZeros() {
        playlist(1, 10, 20); playlist(2, 30);
        score(10, 8.0); score(20, null); score(30, 8.0);
        var result = service.compare(1, 1, 2);
        assertThat(result.left().average()).isEqualTo(8.0);
        assertThat(result.left().ratedMovies()).isEqualTo(1);
        assertThat(result.left().unratedMovies()).isEqualTo(1);
        assertThat(result.outcome()).isEqualTo("TIE");
    }
    @Test void emptyOrUnratedPlaylistHasNoWinner() {
        playlist(1); playlist(2, 30); score(30, null);
        var result = service.compare(1, 1, 2);
        assertThat(result.outcome()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(result.winnerPlaylistId()).isNull();
    }
    @Test void upstreamFailureNeverAwardsAPartialWinner() {
        playlist(1, 10); playlist(2, 30); score(10, 9.0);
        when(scores.score(30, null)).thenThrow(MovieApiException.invalidResponse());
        var result = service.compare(1, 1, 2);
        assertThat(result.outcome()).isEqualTo("INCOMPLETE");
        assertThat(result.right().unavailableMovies()).isEqualTo(1);
        assertThat(result.winnerPlaylistId()).isNull();
    }
    @Test void anotherUsersPlaylistIsRejectedBeforeFetchingMovies() {
        playlist(1, 10);
        when(playlists.findById(2L)).thenReturn(Optional.of(new Playlist(2, "Other user's list", 2, false)));
        assertThatThrownBy(() -> service.compare(1, 1, 2)).isInstanceOf(NoSuchElementException.class);
        verifyNoInteractions(scores);
    }
}
