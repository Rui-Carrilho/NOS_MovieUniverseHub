package com.movieuniverse.hub.playlist;

import com.movieuniverse.hub.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class PlaylistService {

    private final PlaylistRepository repository;
    private final UserRepository users;

    public PlaylistService(
            PlaylistRepository repository,
            UserRepository users
    ) {
        this.repository = repository;
        this.users = users;
    }

    public List<Playlist> findAll(long userId) {
        if (userId <= 0 || users.findById(userId).isEmpty()) {
            throw new NoSuchElementException("User not found");
        }

        return repository.findAll(userId);
    }

    public Optional<Playlist> findById(long id) {
        return repository.findById(id);
    }

    public Playlist create(String name, Long userId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Playlist name must not be blank"
            );
        }

        String normalizedName = name.strip();

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException(
                    "Playlist name must not exceed 100 characters"
            );
        }

        if (userId == null || userId <= 0 ||
                users.findById(userId).isEmpty()) {
            throw new IllegalArgumentException(
                    "A valid userId is required"
            );
        }

        return repository.create(normalizedName, userId);
    }

    public void setDeleted(long id, long userId, boolean deleted) {
        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "A valid userId is required"
            );
        }

        if (repository.setDeleted(id, userId, deleted) == 0) {
            throw new NoSuchElementException("Playlist not found");
        }
    }
}