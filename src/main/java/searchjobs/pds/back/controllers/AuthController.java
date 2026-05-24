package searchjobs.pds.back.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import searchjobs.pds.back.dto.AuthResponse;
import searchjobs.pds.back.dto.LoginRequest;
import searchjobs.pds.back.dto.RegisterRequest;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.services.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal User usuario) {
        if (usuario == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(new AuthResponse(null, usuario.getEmail(), usuario.getNome()));
    }
}