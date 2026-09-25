package com.movieuniverse.hub.auth;

import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.regex.Pattern;

@Configuration
public class OwnershipConfiguration implements WebMvcConfigurer {
    private final CurrentUser current;
    private final JdbcTemplate jdbc;
    public OwnershipConfiguration(CurrentUser current, JdbcTemplate jdbc) { this.current = current; this.jdbc = jdbc; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            private final Pattern playlist = Pattern.compile("^/api/playlists/([0-9]+)(?:/.*)?$");
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String[] supplied = request.getParameterValues("userId");
                if (supplied != null) {
                    if (supplied.length != 1) throw new IllegalArgumentException("userId inválido.");
                    try { current.require(Long.valueOf(supplied[0])); }
                    catch (NumberFormatException exception) { throw new IllegalArgumentException("userId inválido."); }
                }
                var match = playlist.matcher(request.getRequestURI());
                if (match.matches()) {
                    long owner = current.required().id();
                    Long id;
                    try { id = Long.valueOf(match.group(1)); }
                    catch (NumberFormatException e) { throw new IllegalArgumentException("Playlist inválida."); }
                    Integer count = jdbc.queryForObject("SELECT count(*) FROM playlist WHERE id = ? AND user_id = ?",
                            Integer.class, id, owner);
                    if (count == null || count == 0)
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist não encontrada.");
                }
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
