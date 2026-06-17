package searchjobs.pds.back.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import searchjobs.pds.back.dto.AuthResponse;
import searchjobs.pds.back.dto.LoginRequest;
import searchjobs.pds.back.dto.RegisterRequest;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.repositories.UserRepository;
import searchjobs.pds.back.security.JwtUtil;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        User user = new User();
        user.setNome(request.nome());
        user.setEmail(request.email());
        user.setSenhaHash(passwordEncoder.encode(request.senha()));
        user.setSenioridadeAlvo(request.senioridadeAlvo());

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return UserService.toAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String token = jwtUtil.generateToken(user.getEmail());
        return UserService.toAuthResponse(token, user);
    }
}