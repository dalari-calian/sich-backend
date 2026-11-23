package com.sich.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @Schema(description = "Senha atual", example = "senhaAtual123")
        @NotBlank String currentPassword,

        @Schema(description = "Nova senha (mínimo 8 caracteres, diferente da atual)", example = "novaSenha456")
        @NotBlank @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres") String newPassword) {
}
