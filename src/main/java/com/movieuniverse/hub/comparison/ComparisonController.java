package com.movieuniverse.hub.comparison;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comparisons")
public class ComparisonController {
    private final ComparisonService service;
    public ComparisonController(ComparisonService service) { this.service = service; }

    @GetMapping
    public ComparisonService.Comparison compare(@RequestParam("userId") long userId,
            @RequestParam("left") long left, @RequestParam("right") long right) {
        return service.compare(userId, left, right);
    }
}
