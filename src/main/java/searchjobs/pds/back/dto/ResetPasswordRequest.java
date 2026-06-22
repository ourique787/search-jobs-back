package searchjobs.pds.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$",
            message = "Senha deve ter no mínimo 8 caracteres, uma letra maiúscula e um símbolo"
        ) String novaSenha
) {}
