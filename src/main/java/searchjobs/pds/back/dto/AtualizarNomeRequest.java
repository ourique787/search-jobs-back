package searchjobs.pds.back.dto;

import jakarta.validation.constraints.NotBlank;

public record AtualizarNomeRequest(
        @NotBlank(message = "Nome não pode ser vazio") String nome
) {}
