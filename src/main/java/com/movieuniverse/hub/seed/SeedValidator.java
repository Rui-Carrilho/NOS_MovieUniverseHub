package com.movieuniverse.hub.seed;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class SeedValidator {

    private record RatingKey(String username, long tmdbId) {
    }

    public void validate(SeedData seed) {
        require(seed != null, "Seed file is empty");
        require("1.0".equals(seed.versao()), "Unsupported seed version");
        require(
                seed.utilizadores() != null
                        && seed.playlists() != null
                        && seed.notas() != null,
                "Users, playlists and ratings are required"
        );

        Set<String> usernames = new HashSet<>();

        for (SeedData.User user : seed.utilizadores()) {
            require(user != null, "Null user");
            requireText(user.nome(), 80, "username");
            require(
                    usernames.add(user.nome()),
                    "Duplicate username: " + user.nome()
            );
        }

        Set<String> playlistIds = new HashSet<>();

        for (SeedData.Playlist playlist : seed.playlists()) {
            require(playlist != null, "Null playlist");
            requireText(playlist.id(), 100, "playlist ID");
            requireText(playlist.nome(), 100, "playlist name");

            require(
                    playlistIds.add(playlist.id()),
                    "Duplicate playlist ID: " + playlist.id()
            );
            require(
                    usernames.contains(playlist.utilizador()),
                    "Unknown playlist owner"
            );
            require(
                    playlist.apagada() != null
                            && playlist.filmes() != null,
                    "Playlist deleted flag and movies are required"
            );

            Set<Long> seenMovies = new HashSet<>();
            Set<Integer> occupiedPositions = new HashSet<>();

            for (SeedData.Movie movie : playlist.filmes()) {
                require(movie != null, "Null movie");
                require(
                        movie.tmdb_id() != null && movie.tmdb_id() > 0,
                        "Invalid TMDB ID"
                );
                require(
                        movie.ordem() != null && movie.ordem() > 0,
                        "Invalid movie position"
                );

                // A repeated movie is intentional in the supplied seed.
                // Only the first occurrence will be imported.
                if (seenMovies.add(movie.tmdb_id())) {
                    require(
                            occupiedPositions.add(movie.ordem()),
                            "Different movies share a position"
                    );
                }
            }
        }

        Set<RatingKey> ratingKeys = new HashSet<>();

        for (SeedData.Rating rating : seed.notas()) {
            require(rating != null, "Null rating");
            require(
                    usernames.contains(rating.utilizador()),
                    "Unknown rating owner"
            );
            require(
                    rating.tmdb_id() != null
                            && rating.tmdb_id() > 0,
                    "Invalid rating movie ID"
            );
            require(
                    rating.estrelas() != null
                            && rating.estrelas() >= 1
                            && rating.estrelas() <= 10,
                    "Rating must be between 1 and 10"
            );
            require(
                    rating.data() != null,
                    "Rating date is required"
            );
            require(
                    ratingKeys.add(new RatingKey(
                            rating.utilizador(),
                            rating.tmdb_id()
                    )),
                    "Duplicate user/movie rating"
            );
        }
    }

    private void requireText(
            String value,
            int maximumLength,
            String field
    ) {
        require(
                value != null
                        && !value.isBlank()
                        && value.equals(value.strip())
                        && value.length() <= maximumLength,
                "Invalid " + field
        );
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}