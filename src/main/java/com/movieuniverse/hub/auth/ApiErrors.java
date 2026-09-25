package com.movieuniverse.hub.auth;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.*;
import java.util.*;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",
                error.getReason() == null ? "Pedido inválido." : error.getReason()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
    }
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String,String>> missing(NoSuchElementException error) {
        return ResponseEntity.status(404).body(Map.of("message", error.getMessage()));
    }
}
