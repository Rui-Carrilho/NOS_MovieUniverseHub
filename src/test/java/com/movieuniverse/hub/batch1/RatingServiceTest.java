package com.movieuniverse.hub.batch1;

import com.movieuniverse.hub.movie.MovieService;
import com.movieuniverse.hub.rating.*;
import com.movieuniverse.hub.user.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RatingServiceTest {
    private final RatingRepository repository = mock(RatingRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MovieService movies = mock(MovieService.class);
    private final TransactionTemplate transactions = new TransactionTemplate(new AbstractPlatformTransactionManager() {
        protected Object doGetTransaction() { return new Object(); }
        protected void doBegin(Object transaction, TransactionDefinition definition) {}
        protected void doCommit(DefaultTransactionStatus status) {}
        protected void doRollback(DefaultTransactionStatus status) {}
    });
    private final RatingService service = new RatingService(repository, users, movies, transactions);

    @Test void validRatingValidatesMovieAndWritesInTransaction() {
        when(users.findById(1)).thenReturn(Optional.of(new AppUser(1, "ana")));
        service.save(1, 27205, 9);
        var order = inOrder(movies, repository);
        order.verify(movies).details(27205);
        order.verify(repository).save(1, 27205, 9);
    }
    @Test void invalidStarsNeverCallTmdbOrWrite() {
        when(users.findById(1)).thenReturn(Optional.of(new AppUser(1, "ana")));
        assertThatThrownBy(() -> service.save(1, 1, 11)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save(1, 1, null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(movies, repository);
    }
    @Test void removingRatingDoesNotRequireTmdb() {
        when(users.findById(1)).thenReturn(Optional.of(new AppUser(1, "ana")));
        service.remove(1, 27205);
        verify(repository).remove(1, 27205);
        verifyNoInteractions(movies);
    }
}
