package searchjobs.pds.back.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchjobs.pds.back.dto.UserResponse;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.services.UserService;

import java.util.List;

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

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getNome(), user.getEmail(),
                user.getSenioridadeAlvo(), user.getStacksInteresse());
    }
}