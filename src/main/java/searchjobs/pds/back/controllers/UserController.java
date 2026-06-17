package searchjobs.pds.back.controllers;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import searchjobs.pds.back.dto.AtualizarPerfilRequest;
import searchjobs.pds.back.dto.AtualizarSenhaRequest;
import searchjobs.pds.back.dto.AuthResponse;
import searchjobs.pds.back.dto.UserResponse;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.services.UserService;

import java.io.IOException;
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
                usuario.getUsername(),
                request.nome(),
                request.linkedin(),
                request.github(),
                request.senioridadeAlvo(),
                request.stackIds());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> atualizarFoto(
            @AuthenticationPrincipal User usuario,
            @RequestPart("foto") MultipartFile foto) {
        try {
            AuthResponse response = userService.atualizarFotoPerfil(usuario.getUsername(), foto);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Erro ao salvar foto."));
        }
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
                user.getFotoPerfil(), user.getSenioridadeAlvo(), user.getStacksPreferidas());
    }
}
