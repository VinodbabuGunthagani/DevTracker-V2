package com.devtracker.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devtracker.backend.model.User;
import com.devtracker.backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import  jakarta.servlet.http.HttpServletResponse;
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ============================================================
    // REGISTER
    // ============================================================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        boolean result = authService.register(
                request.name(),
                request.email(),
                request.password()
        );

        if (result) {

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Registration successful"
                    )
            );
        }

        return ResponseEntity.badRequest().body(
                Map.of(
                        "success", false,
                        "message",
                        "Registration failed. Please try again."
                )
        );
    }


    // ============================================================
    // LOGIN
    // ============================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        boolean result = authService.login(
                request.email(),
                request.password(),
                request.remember(),
                httpRequest,
                httpResponse
        );

        if (result) {

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Login successful"
                    )
            );
        }

        return ResponseEntity
                .status(401)
                .body(
                        Map.of(
                                "success", false,
                                "message",
                                "Invalid email or password."
                        )
                );
    }


    // ============================================================
    // REQUEST RECORDS
    // ============================================================

    public record RegisterRequest(
            String name,
            String email,
            String password
    ) {
    }


    public record LoginRequest(
            String email,
            String password,
            boolean remember
    ) {
    }
    @GetMapping("/session")
public ResponseEntity<?> checkSession(
        HttpServletRequest request,
        HttpServletResponse response) {

    User user =
            authService.checkSession(request, response);

    if (user != null) {

        return ResponseEntity.ok(
                Map.of(
                        "authenticated", true,
                        "name", user.getName()
                )
        );
    }

    return ResponseEntity.ok(
            Map.of(
                    "authenticated", false
            )
    );
}
@PostMapping("/logout")
public ResponseEntity<?> logout(
        HttpServletRequest request,
        HttpServletResponse response) {

    authService.logout(request, response);

    return ResponseEntity.ok(
            Map.of(
                    "success", true,
                    "message", "Logged out successfully"
            )
    );
}

}