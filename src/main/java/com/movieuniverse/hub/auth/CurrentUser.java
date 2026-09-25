package com.movieuniverse.hub.auth;
import com.movieuniverse.hub.user.AppUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class CurrentUser {
    private final AccountService accounts;
    public CurrentUser(AccountService accounts) { this.accounts = accounts; }
    public AppUser optional() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken)
            return null;
        return accounts.find(authentication.getName());
    }
    public AppUser required() {
        var user = optional();
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão.");
        return user;
    }
    public void require(Long id) {
        if (id == null || id.longValue() != required().id())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este perfil não pertence à sessão.");
    }
}
