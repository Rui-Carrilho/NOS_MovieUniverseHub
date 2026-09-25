package com.movieuniverse.hub.user;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<AppUser> findAll() {
        return repository.findAll();
    }

    public Optional<AppUser> findById(long id) {
        return repository.findById(id);
    }

    public AppUser create(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username must not be blank"
            );
        }

        String normalized = username.strip();

        if (normalized.length() > 80) {
            throw new IllegalArgumentException(
                    "Username must not exceed 80 characters"
            );
        }

        return repository.create(normalized)
                .orElseThrow(() ->
                        new UsernameTakenException(
                                "Username already exists"
                        )
                );
    }

    public static class UsernameTakenException extends RuntimeException {
        public UsernameTakenException(String message) {
            super(message);
        }
    }
}