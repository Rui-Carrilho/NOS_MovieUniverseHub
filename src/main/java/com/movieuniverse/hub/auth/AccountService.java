package com.movieuniverse.hub.auth;

import com.movieuniverse.hub.user.AppUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;

@Service
public class AccountService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    public AccountService(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }
    public static String username(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 80)
            throw new IllegalArgumentException("O nome deve ter entre 1 e 80 caracteres.");
        return value.strip();
    }
    public static void password(String value) {
        if (value == null || value.length() < 10 || value.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("A palavra-passe deve ter pelo menos 10 caracteres e no máximo 72 bytes.");
    }
    @Override public UserDetails loadUserByUsername(String username) {
        return jdbc.query("SELECT username, password_hash FROM app_user WHERE username = ? AND password_hash IS NOT NULL",
                (row, n) -> User.withUsername(row.getString("username"))
                        .password(row.getString("password_hash")).roles("USER").build(), username)
                .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }
    @Transactional
    public AppUser register(String name, String rawPassword) {
        String normalized = username(name);
        password(rawPassword);
        var users = jdbc.query("""
                INSERT INTO app_user(username, password_hash) VALUES (?, ?)
                ON CONFLICT (username) DO NOTHING RETURNING id, username
                """, (row, n) -> new AppUser(row.getLong("id"), row.getString("username")),
                normalized, encoder.encode(rawPassword));
        if (users.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este nome já existe.");
        return users.getFirst();
    }
    public AppUser find(String username) {
        return jdbc.queryForObject("SELECT id, username FROM app_user WHERE username = ?",
                (row, n) -> new AppUser(row.getLong("id"), row.getString("username")), username);
    }
    // Only called by the local command runner, never exposed through HTTP.
    public void setLocalPassword(String name, String rawPassword) {
        password(rawPassword);
        if (jdbc.update("UPDATE app_user SET password_hash = ? WHERE username = ?",
                encoder.encode(rawPassword), username(name)) != 1)
            throw new IllegalArgumentException("Perfil inexistente. Importa os dados primeiro.");
    }
}
