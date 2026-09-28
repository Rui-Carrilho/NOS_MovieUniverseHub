package com.movieuniverse.hub.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Page routing is separate from JSON API authentication responses. */
@Controller
public class PageController {
    private final CurrentUser current;

    public PageController(CurrentUser current) { this.current = current; }

    @GetMapping({"/login", "/register"})
    public String authenticationPage() {
        return current.optional() == null ? "forward:/auth.html" : "redirect:/";
    }

    @GetMapping("/dashboard")
    public String dashboard() { return "redirect:/"; }
}
