package com.sich.product.dto;

import java.math.BigDecimal;

import com.sich.common.util.DurationFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @Schema(description = "Id da categoria (cadastrada pelo admin) à qual o produto pertence", example = "1")
        @NotNull Long categoryId,

        @Schema(description = "Nome do produto/serviço", example = "Alongamento em gel")
        @NotBlank @Size(max = 120) String name,

        @Schema(description = "Como o prestador realiza o serviço", example = "Cutilagem, preparação da unha e aplicação do gel")
        String description,

        @Schema(description = "Preço cobrado pelo prestador", example = "120.00")
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal price,

        @Schema(description = "Tempo médio do serviço no formato HH:MM (máximo 99:59)", example = "01:30")
        @NotBlank @Pattern(regexp = DurationFormat.PATTERN, message = "deve estar no formato HH:MM (ex.: 01:30)")
        String duration) {
}
