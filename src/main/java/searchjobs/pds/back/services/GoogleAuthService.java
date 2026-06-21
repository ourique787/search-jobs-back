package searchjobs.pds.back.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import searchjobs.pds.back.dto.AuthResponse;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.repositories.UserRepository;
import searchjobs.pds.back.security.JwtUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class GoogleAuthService {

    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleAuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse loginWithGoogle(String accessToken) {
        Map<String, Object> userInfo = fetchUserInfo(accessToken);

        String email = (String) userInfo.get("email");
        String nome = (String) userInfo.get("name");

        if (email == null) {
            throw new IllegalArgumentException("Não foi possível obter o email da conta Google.");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNome(nome != null ? nome : email.split("@")[0]);
            return userRepository.save(newUser);
        });

        String token = jwtUtil.generateToken(user.getEmail());
        return UserService.toAuthResponse(token, user);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchUserInfo(String accessToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USERINFO_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("Token Google inválido ou expirado.");
            }

            return objectMapper.readValue(response.body(), Map.class);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao verificar token Google.", e);
        }
    }
}
