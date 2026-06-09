package searchjobs.pds.back.dto;

public record AuthResponse(String token, String email, String nome, String linkedin, String github) {}