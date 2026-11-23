package com.sich.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Endereço do cliente ou prestador. Campo ausente (ou {@code null}) mantém o valor
 * atual e campo vazio apaga, então a tela pode mandar só o que exibe. O CEP é salvo
 * só com os números e, quando informado, precisa ter 8 dígitos.
 */
public record AddressRequest(
        @Schema(description = "CEP, com ou sem traço", example = "01310-100")
        @Size(max = 20) String cep,

        @Schema(description = "Cidade", example = "São Paulo")
        @Size(max = 255) String city,

        @Schema(description = "Estado", example = "SP")
        @Size(max = 255) String state,

        @Schema(description = "Rua", example = "Avenida Paulista")
        @Size(max = 255) String street,

        @Schema(description = "Número (ou s/n)", example = "1000")
        @Size(max = 255) String numberAdress,

        @Schema(description = "Complemento", example = "Apto 12")
        @Size(max = 255) String complementAdress,

        @Schema(description = "Bairro", example = "Bela Vista")
        @Size(max = 255) String neighborhood) {
}
