package searchjobs.pds.back.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import searchjobs.pds.back.dto.AuthResponse;
import searchjobs.pds.back.entities.Senioridade;
import searchjobs.pds.back.entities.Stack;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.repositories.StackRepository;
import searchjobs.pds.back.repositories.UserRepository;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StackRepository stackRepository;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;

    public UserService(UserRepository userRepository, StackRepository stackRepository,
                       PasswordEncoder passwordEncoder, Cloudinary cloudinary) {
        this.userRepository = userRepository;
        this.stackRepository = stackRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinary = cloudinary;
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

    @SuppressWarnings("unchecked")
    public AuthResponse atualizarFotoPerfil(String email, MultipartFile foto) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String publicId = "searchjobs/fotos/" + email.replaceAll("[^a-zA-Z0-9]", "_");
        Map uploadResult = cloudinary.uploader().upload(foto.getBytes(), ObjectUtils.asMap(
                "public_id", publicId,
                "overwrite", true,
                "resource_type", "image"
        ));

        user.setFotoPerfil((String) uploadResult.get("secure_url"));
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
