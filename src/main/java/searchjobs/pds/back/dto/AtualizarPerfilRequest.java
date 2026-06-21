package searchjobs.pds.back.dto;

import searchjobs.pds.back.entities.Senioridade;

import java.util.List;

public record AtualizarPerfilRequest(
        String nome,
        String linkedin,
        String github,
        Senioridade senioridadeAlvo,
        List<Long> stackIds
) {}
