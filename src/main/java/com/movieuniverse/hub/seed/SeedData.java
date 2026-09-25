package com.movieuniverse.hub.seed;

import java.time.LocalDate;
import java.util.List;

public record SeedData(
        String versao,
        String descricao,
        List<User> utilizadores,
        List<Playlist> playlists,
        List<Rating> notas
) {
    public record User(String nome) {
    }

    public record Playlist(
            String id,
            String nome,
            String utilizador,
            Boolean apagada,
            List<Movie> filmes
    ) {
    }

    public record Movie(
            Long tmdb_id,
            Integer ordem
    ) {
    }

    public record Rating(
            String utilizador,
            Long tmdb_id,
            Integer estrelas,
            LocalDate data
    ) {
    }
}