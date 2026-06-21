package searchjobs.pds.back.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import searchjobs.pds.back.dto.*;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.services.AuthService;
import searchjobs.pds.back.services.GoogleAuthService;
import searchjobs.pds.back.services.PasswordResetService;
import searchjobs.pds.back.services.UserService;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final GoogleAuthService googleAuthService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
                          GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.googleAuthService = googleAuthService;
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
        return ResponseEntity.ok(UserService.toAuthResponse(null, usuario));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            return ResponseEntity.ok(googleAuthService.loginWithGoogle(request.accessToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.solicitarReset(request.email());
        return ResponseEntity.ok(Map.of("message", "Se o email existir, você receberá as instruções em breve."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.redefinirSenha(request.token(), request.novaSenha());
            return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}