package searchjobs.pds.back.services;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import searchjobs.pds.back.entities.PasswordResetToken;
import searchjobs.pds.back.entities.User;
import searchjobs.pds.back.repositories.PasswordResetTokenRepository;
import searchjobs.pds.back.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void solicitarReset(String email) {
        System.out.println("📧 [Email] Solicitação de reset para: " + email);
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            System.out.println("📧 [Email] Usuário encontrado: " + user.getNome());
            tokenRepository.deleteByUser(user);

            String token = UUID.randomUUID().toString();
            LocalDateTime expiry = LocalDateTime.now().plusHours(1);
            tokenRepository.save(new PasswordResetToken(token, user, expiry));

            new Thread(() -> {
                try {
                    System.out.println("📧 [Email] Enviando para: " + user.getEmail());
                    enviarEmail(user, token);
                    System.out.println("✅ [Email] Enviado com sucesso para: " + user.getEmail());
                } catch (Exception e) {
                    System.err.println("❌ [Email] Erro ao enviar: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        }, () -> System.out.println("⚠️ [Email] Email não encontrado no banco: " + email));
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Token já utilizado");
        }
        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Token expirado");
        }

        User user = resetToken.getUser();
        user.setSenhaHash(passwordEncoder.encode(novaSenha));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private void enviarEmail(User user, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String texto = "Olá, " + user.getNome() + "!\n\n" +
                "Recebemos uma solicitação para redefinir sua senha.\n\n" +
                "Clique no link abaixo para criar uma nova senha (válido por 1 hora):\n" +
                link + "\n\n" +
                "Se você não solicitou isso, ignore este email.\n\n" +
                "SearchJobs";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = Map.of(
                "from", "SearchJobs <onboarding@resend.dev>",
                "to", List.of(user.getEmail()),
                "subject", "Redefinição de senha - SearchJobs",
                "text", texto
        );

        restTemplate.exchange(
                "https://api.resend.com/emails",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
    }
}
