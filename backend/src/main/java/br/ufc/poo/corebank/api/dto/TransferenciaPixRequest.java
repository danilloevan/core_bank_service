package br.ufc.poo.corebank.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferenciaPixRequest(
        @NotNull(message = "O ID da conta de origem do PIX é obrigatório.")
        UUID contaOrigemId,

        @NotBlank(message = "A chave PIX de destino é obrigatória.")
        String chavePixDestino,

        @NotNull(message = "O valor da transferência PIX é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor do PIX deve ser de no mínimo R$ 0,01.")
        BigDecimal valor,

        String descricao
) {
}
