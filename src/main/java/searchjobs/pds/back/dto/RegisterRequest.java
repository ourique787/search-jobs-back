package searchjobs.pds.back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import searchjobs.pds.back.entities.Senioridade;

public record RegisterRequest(
        @NotBlank(message = "Nome é obrigatório") String nome,
        @NotBlank @Email(message = "Email inválido") String email,
        @NotBlank @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres") String senha,
        Senioridade senioridadeAlvo
) {}