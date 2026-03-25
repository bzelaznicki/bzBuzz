package com.zelaznicki.bzBuzz.common;

import com.zelaznicki.bzBuzz.vote.VoteController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = VoteController.class)
public class RestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException e) {
        String message = e.getMessage() != null ? e.getMessage() : "Resource not found";
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException e) {
        String message = e.getMessage() != null ? e.getMessage() : "Bad request";
        return ResponseEntity.badRequest()
                .body(Map.of("error", message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        String message = e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : "Access denied";
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException e) {
        String reason = e.getReason() != null ? e.getReason() : "An error occurred";
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("error", reason));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "An unexpected error occurred"));
    }
}