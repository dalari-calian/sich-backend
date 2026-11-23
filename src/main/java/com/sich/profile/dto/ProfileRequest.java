package com.sich.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * "Meus dados". Campo ausente (ou {@code null}) mantém o valor atual; {@code cnpjCpf}
 * vazio apaga o documento. Para o administrador, só {@code name} é considerado.
 */
public record ProfileRequest(
        @Schema(description = "Nome exibido no perfil (não pode ser vazio)", example = "Júlia Prado")
        @Size(max = 255) String name,

        @Schema(description = "Telefone (não pode ser vazio)", example = "11988887777")
        @Size(max = 255) String phone,

        @Schema(description = "CPF ou CNPJ; vazio apaga", example = "12345678900")
        @Size(max = 255) String cnpjCpf) {
}
