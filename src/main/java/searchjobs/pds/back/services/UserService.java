package searchjobs.pds.back.services;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import searchjobs.pds.back.dto.AuthResponse;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> listarTodos() {
        return userRepository.findAll();
    }

    public Optional<User> buscarPorEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public AuthResponse atualizarPerfil(String email, String nome, String linkedin, String github) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        if (nome != null) user.setNome(nome);
        if (linkedin != null) user.setLinkedin(linkedin);
        if (github != null) user.setGithub(github);
        userRepository.save(user);
        return new AuthResponse(null, user.getEmail(), user.getNome(), user.getLinkedin(), user.getGithub());
    }

    public void atualizarSenha(String email, String senhaAtual, String novaSenha) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        if (!passwordEncoder.matches(senhaAtual, user.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        user.setSenhaHash(passwordEncoder.encode(novaSenha));
        userRepository.save(user);
    }
}