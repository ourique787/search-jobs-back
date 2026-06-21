package searchjobs.pds.back.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(@NotBlank String accessToken) {}
