package com.movieuniverse.hub.auth;

import com.movieuniverse.hub.user.AppUser;
import com.github.benmanes.caffeine.cache.*;
import jakarta.servlet.http.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;

@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Credentials(String username, String password) {}
    public record Session(AppUser user, String csrfToken, String csrfHeader) {}
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final AccountService accounts;
    private final CurrentUser current;
    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
            .maximumSize(10000).expireAfterWrite(Duration.ofMinutes(1)).build();

    public AuthController(AuthenticationManager manager, SecurityContextRepository contexts,
            AccountService accounts, CurrentUser current) {
        this.manager = manager; this.contexts = contexts; this.accounts = accounts; this.current = current;
    }
    @GetMapping("/session")
    public Session session(CsrfToken csrf) {
        return new Session(current.optional(), csrf.getToken(), csrf.getHeaderName());
    }
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody Credentials input, HttpServletRequest request,
            HttpServletResponse response) {
        limit(request);
        accounts.register(input.username(), input.password());
        authenticate(input, request, response);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody Credentials input, HttpServletRequest request,
            HttpServletResponse response) {
        limit(request);
        authenticate(input, request, response);
        return ResponseEntity.noContent().build();
    }
    private void authenticate(Credentials input, HttpServletRequest request, HttpServletResponse response) {
        AccountService.password(input.password());
        var authentication = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                AccountService.username(input.username()), input.password()));
        // Replace anonymous/previous sessions and their CSRF tokens; never retain a supplied session ID.
        var old = request.getSession(false);
        if (old != null) old.invalidate();
        request.getSession(true);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
    private void limit(HttpServletRequest request) {
        if (attempts.get(request.getRemoteAddr(), key -> new AtomicInteger()).incrementAndGet() > 10)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Tenta novamente dentro de um minuto.");
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<java.util.Map<String,String>> invalidCredentials() {
        return ResponseEntity.status(401).body(java.util.Map.of("message", "Nome ou palavra-passe incorretos."));
    }
}
