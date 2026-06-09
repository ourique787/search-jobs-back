package searchjobs.pds.back.dto;

import jakarta.validation.constraints.NotBlank;

public record AtualizarSenhaRequest(
        @NotBlank(message = "Senha atual é obrigatória") String senhaAtual,
        @NotBlank(message = "Nova senha é obrigatória") String novaSenha
) {}
