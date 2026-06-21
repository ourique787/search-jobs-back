package searchjobs.pds.back.dto;

import searchjobs.pds.back.entities.Senioridade;
import searchjobs.pds.back.entities.Stack;

import java.util.Set;

public record AuthResponse(
        String token,
        String email,
        String nome,
        String linkedin,
        String github,
        String fotoPerfil,
        Senioridade senioridadeAlvo,
        Set<Stack> stacksPreferidas
) {}
