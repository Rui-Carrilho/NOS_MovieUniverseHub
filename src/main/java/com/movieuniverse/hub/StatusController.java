package com.movieuniverse.hub;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping({"/api/status", "/api/health"})
    public StatusResponse status() {
        return new StatusResponse("MovieUniverse Hub", "running", "0.1.0");
    }

    public record StatusResponse(String application, String status, String version) {
    }
}