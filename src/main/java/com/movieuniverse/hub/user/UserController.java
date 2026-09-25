package com.movieuniverse.hub.user;
import com.movieuniverse.hub.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CurrentUser current;
    public UserController(CurrentUser current) { this.current = current; }
    @GetMapping public List<AppUser> findAll() { return List.of(current.required()); }
    @GetMapping("/{id}") public AppUser findById(@PathVariable("id") long id) {
        current.require(id); return current.required();
    }
}
