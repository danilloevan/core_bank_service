package br.ufc.poo.corebank.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositoRequest(
        @NotNull(message = "O ID da conta de destino do depósito é obrigatório.")
        UUID contaDestinoId,

        @NotNull(message = "O valor do depósito é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor do depósito deve ser de no mínimo R$ 0,01.")
        BigDecimal valor,

        @NotBlank(message = "O canal de origem do depósito é obrigatório.")
        String canalOrigem
) {
}
