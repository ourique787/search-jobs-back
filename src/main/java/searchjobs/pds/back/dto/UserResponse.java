package searchjobs.pds.back.dto;

import searchjobs.pds.back.entities.Senioridade;
import searchjobs.pds.back.entities.Stack;

import java.util.Set;

public record UserResponse(Long id, String nome, String email, Senioridade senioridadeAlvo, Set<Stack> stacksInteresse) {}