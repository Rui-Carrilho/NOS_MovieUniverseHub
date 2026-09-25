package com.movieuniverse.hub.rating;

import com.movieuniverse.hub.movie.MovieService;
import com.movieuniverse.hub.user.UserRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RatingService {
    private final RatingRepository ratings;
    private final UserRepository users;
    private final MovieService movies;
    private final TransactionTemplate transactions;

    public RatingService(RatingRepository ratings, UserRepository users, MovieService movies,
            TransactionTemplate transactions) {
        this.ratings = ratings;
        this.users = users;
        this.movies = movies;
        this.transactions = transactions;
    }

    public void save(long userId, long tmdbId, Integer stars) {
        requireUser(userId);
        if (tmdbId <= 0 || stars == null || stars < 1 || stars > 10) {
            throw new IllegalArgumentException("A nota deve ser um número inteiro entre 1 e 10 e o filme deve ser válido.");
        }
        // Verify the movie before opening a transaction: do not hold a DB connection during network I/O.
        movies.details(tmdbId);
        transactions.executeWithoutResult(status -> ratings.save(userId, tmdbId, stars));
    }

    public void remove(long userId, long tmdbId) {
        requireUser(userId);
        if (tmdbId <= 0) throw new IllegalArgumentException("Identificador de filme inválido.");
        // Removing one's own rating works even when TMDB is offline.
        transactions.executeWithoutResult(status -> ratings.remove(userId, tmdbId));
    }

    public void requireUser(long userId) {
        if (userId <= 0 || users.findById(userId).isEmpty()) throw new NoSuchElementException("Perfil não encontrado.");
    }
}
