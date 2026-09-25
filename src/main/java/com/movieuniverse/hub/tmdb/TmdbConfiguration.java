package com.movieuniverse.hub.tmdb;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TmdbConfiguration {
    @Bean
    RestClient tmdbRestClient(
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.api-token:}") String token,
            @Value("${tmdb.connect-timeout:3s}") Duration connectTimeout,
            @Value("${tmdb.read-timeout:10s}") Duration readTimeout) {
        var http = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(readTimeout);
        var builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (!token.isBlank()) builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.strip());
        return builder.build();
    }
}
