package searchjobs.pds.back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import searchjobs.pds.back.entities.Senioridade;

public record RegisterRequest(
        @NotBlank(message = "Nome é obrigatório") String nome,
        @NotBlank @Email(message = "Email inválido") String email,
        @NotBlank
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$",
            message = "Senha deve ter no mínimo 8 caracteres, uma letra maiúscula e um símbolo"
        ) String senha,
        Senioridade senioridadeAlvo
) {}