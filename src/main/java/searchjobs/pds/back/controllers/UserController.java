package searchjobs.pds.back.controllers;

import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> salvar(@RequestBody User user){
        return ResponseEntity.ok(userService.salvar(user));
    }

    @GetMapping
    public ResponseEntity<List<User>> listarTodos() {
        return ResponseEntity.ok(userService.listarTodos());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> buscarPorEmail(@PathVariable String email){
        return userService.buscarPorEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
