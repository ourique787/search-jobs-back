package searchjobs.pds.back.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import searchjobs.pds.back.dto.AuthResponse;
import searchjobs.pds.back.entities.Senioridade;
import searchjobs.pds.back.entities.Stack;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.repositories.StackRepository;
import searchjobs.pds.back.repositories.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StackRepository stackRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UserService(UserRepository userRepository, StackRepository stackRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.stackRepository = stackRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> listarTodos() {
        return userRepository.findAll();
    }

    public Optional<User> buscarPorEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public AuthResponse atualizarPerfil(String email, String nome, String linkedin, String github,
                                        Senioridade senioridadeAlvo, List<Long> stackIds) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        if (nome != null) user.setNome(nome);
        if (linkedin != null) user.setLinkedin(linkedin);
        if (github != null) user.setGithub(github);
        if (senioridadeAlvo != null) user.setSenioridadeAlvo(senioridadeAlvo);

        if (stackIds != null) {
            Set<Stack> stacks = new HashSet<>(stackRepository.findAllById(stackIds));
            user.setStacksPreferidas(stacks);
        }

        userRepository.save(user);
        return toAuthResponse(null, user);
    }

    public AuthResponse atualizarFotoPerfil(String email, MultipartFile foto) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String extensao = StringUtils.getFilenameExtension(foto.getOriginalFilename());
        String nomeArquivo = UUID.randomUUID() + (extensao != null ? "." + extensao : "");

        Path diretorio = Paths.get("uploads/fotos");
        Files.createDirectories(diretorio);
        Files.write(diretorio.resolve(nomeArquivo), foto.getBytes());

        user.setFotoPerfil(baseUrl + "/uploads/fotos/" + nomeArquivo);
        userRepository.save(user);

        return toAuthResponse(null, user);
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

    public static AuthResponse toAuthResponse(String token, User user) {
        return new AuthResponse(
                token,
                user.getEmail(),
                user.getNome(),
                user.getLinkedin(),
                user.getGithub(),
                user.getFotoPerfil(),
                user.getSenioridadeAlvo(),
                user.getStacksPreferidas()
        );
    }
}
