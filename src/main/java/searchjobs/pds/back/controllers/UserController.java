package searchjobs.pds.back.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import searchjobs.pds.back.dto.AtualizarPerfilRequest;
import searchjobs.pds.back.dto.AtualizarSenhaRequest;
import searchjobs.pds.back.dto.AuthResponse;
import searchjobs.pds.back.dto.UserResponse;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.services.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listarTodos() {
        List<UserResponse> users = userService.listarTodos().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> buscarPorEmail(@PathVariable String email) {
        return userService.buscarPorEmail(email)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<AuthResponse> atualizarPerfil(
            @AuthenticationPrincipal User usuario,
            @RequestBody AtualizarPerfilRequest request) {
        AuthResponse response = userService.atualizarPerfil(
                usuario.getUsername(), request.nome(), request.linkedin(), request.github());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/senha")
    public ResponseEntity<?> atualizarSenha(
            @AuthenticationPrincipal User usuario,
            @Valid @RequestBody AtualizarSenhaRequest request) {
        try {
            userService.atualizarSenha(usuario.getUsername(), request.senhaAtual(), request.novaSenha());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getNome(), user.getEmail(),
                user.getSenioridadeAlvo(), user.getStacksInteresse());
    }
}