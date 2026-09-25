package com.movieuniverse.hub.auth;

import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.*;
import org.springframework.security.web.context.*;
import org.springframework.http.HttpMethod;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {
    @Bean AuthenticationManager authenticationManager(AccountService accounts,
            org.springframework.security.crypto.password.PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(accounts);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
    @Bean SecurityFilterChain security(HttpSecurity http, SecurityContextRepository repository) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/session", "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/movies/**", "/api/movie-scores/**", "/api/health").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .securityContext(context -> context.securityContextRepository(repository))
                // Default synchronizer CSRF tokens protect all mutations, including login and registration.
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"Inicia sessão.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"Pedido não autorizado. Atualiza a página e tenta novamente.\"}");
                        }))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' https://image.tmdb.org; style-src 'self'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
                .logout(logout -> logout.disable())
                .build();
    }
}
